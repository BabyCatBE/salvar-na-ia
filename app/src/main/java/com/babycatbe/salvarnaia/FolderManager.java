package com.babycatbe.salvarnaia;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class FolderManager {

    public static final String PREFS = "salvar_na_ia";
    public static final String KEY_TREE_URI = "videos_tree_uri";
    private static final String PRIVATE_TRASH_DIR_NAME = "trash";
    private static final String LEGACY_TRASH_DIR_NAME = "Lixeira Salvar na IA";

    public static void saveTreeUri(Context context, Uri uri, int flags) {
        int takeFlags = flags & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        context.getContentResolver().takePersistableUriPermission(uri, takeFlags);
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_TREE_URI, uri.toString())
                .apply();
    }

    public static Uri getTreeUri(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_TREE_URI, "");
        if (raw == null || raw.isEmpty()) return null;
        return Uri.parse(raw);
    }

    public static boolean hasFolderAccess(Context context) {
        Uri tree = getTreeUri(context);
        if (tree == null) return false;
        try {
            Uri root = rootDocumentUri(tree);
            try (Cursor c = context.getContentResolver().query(
                    root,
                    new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID},
                    null,
                    null,
                    null
            )) {
                return c != null && c.moveToFirst();
            }
        } catch (Exception ignored) {
            return false;
        }
    }

    public static String getRootName(Context context) {
        Uri tree = getTreeUri(context);
        if (tree == null) return "";
        return getDisplayName(context, rootDocumentUri(tree));
    }

    public static List<Entry> listRootVideos(Context context) {
        List<Entry> files = new ArrayList<>();
        Uri tree = getTreeUri(context);
        if (tree == null) return files;

        String rootId = DocumentsContract.getTreeDocumentId(tree);
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, rootId);

        String[] projection = {
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                DocumentsContract.Document.COLUMN_SIZE
        };

        try (Cursor c = context.getContentResolver().query(children, projection, null, null, null)) {
            if (c == null) return files;
            while (c.moveToNext()) {
                String docId = c.getString(0);
                String name = c.getString(1);
                String mime = c.getString(2);
                long modified = c.isNull(3) ? 0 : c.getLong(3);
                long size = c.isNull(4) ? 0 : c.getLong(4);

                if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) continue;
                if (!isVideoName(name) && (mime == null || !mime.startsWith("video/"))) continue;

                Uri uri = DocumentsContract.buildDocumentUriUsingTree(tree, docId);
                files.add(new Entry(uri, name, mime, modified, size));
            }
        } catch (Exception ignored) {
        }

        files.sort(Comparator.comparingLong(e -> e.lastModified));
        return files;
    }

    public static Entry findRootVideoBySourceUrl(
            Context context,
            String rawUrl,
            Set<String> excludedUris
    ) {
        String token = sourceTokenFromUrl(rawUrl);
        if (token.isEmpty()) return null;

        for (Entry file : listRootVideos(context)) {
            if (file.size == 0) continue;
            if (excludedUris != null && excludedUris.contains(file.uri.toString())) continue;

            String name = file.name == null ? "" : file.name;
            if (name.toLowerCase().contains(token.toLowerCase())) {
                return file;
            }
        }

        return null;
    }

    private static String sourceTokenFromUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isEmpty()) return "";

        try {
            Uri uri = Uri.parse(rawUrl);
            List<String> segments = uri.getPathSegments();

            for (int i = segments.size() - 1; i >= 0; i--) {
                String segment = segments.get(i);
                if (segment == null || segment.isEmpty()) continue;
                if ("reel".equalsIgnoreCase(segment) ||
                        "p".equalsIgnoreCase(segment) ||
                        "video".equalsIgnoreCase(segment)) {
                    continue;
                }
                if (segment.length() >= 6) return segment;
            }
        } catch (Exception ignored) {
        }

        return "";
    }

    public static Uri rename(Context context, Uri uri, String newName) {
        try {
            Uri renamed = DocumentsContract.renameDocument(context.getContentResolver(), uri, newName);
            return renamed == null ? uri : renamed;
        } catch (Exception ignored) {
            return uri;
        }
    }

    /**
     * Moves a ready video out of the user-facing Download_Videos IA folder and into
     * app-private external storage. The private trash is intentionally not exposed in
     * the user's file picker.
     */
    public static Uri moveToTrash(Context context, Uri fileUri, String fileName) throws Exception {
        File trashDir = getPrivateTrashDir(context);
        File target = uniqueFile(trashDir, fileName);

        copyFromContentUri(context.getContentResolver(), fileUri, target);

        boolean deleted = false;
        try {
            deleted = DocumentsContract.deleteDocument(context.getContentResolver(), fileUri);
        } catch (Exception ignored) {
        }

        if (!deleted) {
            try {
                deleted = context.getContentResolver().delete(fileUri, null, null) > 0;
            } catch (Exception ignored) {
            }
        }

        if (!deleted) {
            target.delete();
            throw new IllegalStateException("O vídeo foi copiado para a lixeira, mas não pôde ser removido da pasta principal");
        }

        return Uri.fromFile(target);
    }

    /**
     * Restores either the new private-trash format (file://) or the legacy beta
     * content:// trash folder format.
     */
    public static Uri restoreFromTrash(Context context, Uri fileUri, String fileName) throws Exception {
        Uri tree = requireTree(context);
        Uri root = rootDocumentUri(tree);

        if ("file".equalsIgnoreCase(fileUri.getScheme())) {
            File source = new File(fileUri.getPath());
            if (!source.exists()) throw new IllegalStateException("Arquivo da lixeira não encontrado");

            String mime = mimeForName(fileName);
            Uri target = DocumentsContract.createDocument(
                    context.getContentResolver(),
                    root,
                    mime,
                    fileName
            );
            if (target == null) throw new IllegalStateException("Não foi possível recriar o vídeo");

            boolean copied = false;
            try (InputStream in = new FileInputStream(source);
                 OutputStream out = context.getContentResolver().openOutputStream(target, "w")) {
                if (out == null) throw new IllegalStateException("Não foi possível abrir o destino");
                copy(in, out);
                copied = true;
            } finally {
                if (!copied) {
                    try {
                        DocumentsContract.deleteDocument(context.getContentResolver(), target);
                    } catch (Exception ignored) {
                    }
                }
            }

            if (!source.delete()) {
                try {
                    DocumentsContract.deleteDocument(context.getContentResolver(), target);
                } catch (Exception ignored) {
                }
                throw new IllegalStateException("O vídeo foi restaurado, mas não pôde ser removido da lixeira");
            }

            return target;
        }

        // Compatibility with the visible trash folder created by v0.2.0 beta.
        Uri legacyTrash = findDirectory(context, tree, root, LEGACY_TRASH_DIR_NAME);
        if (legacyTrash == null) {
            throw new IllegalStateException("Pasta da lixeira antiga não encontrada");
        }
        return moveWithFallback(context, fileUri, legacyTrash, root, fileName);
    }

    public static boolean delete(Context context, Uri uri) {
        if (uri == null) return false;

        if ("file".equalsIgnoreCase(uri.getScheme())) {
            String path = uri.getPath();
            return path != null && new File(path).delete();
        }

        try {
            return DocumentsContract.deleteDocument(context.getContentResolver(), uri);
        } catch (Exception ignored) {
            try {
                return context.getContentResolver().delete(uri, null, null) > 0;
            } catch (Exception ignoredAgain) {
                return false;
            }
        }
    }

    public static boolean exists(Context context, Uri uri) {
        if (uri == null) return false;

        if ("file".equalsIgnoreCase(uri.getScheme())) {
            String path = uri.getPath();
            return path != null && new File(path).isFile();
        }

        try (Cursor c = context.getContentResolver().query(
                uri,
                new String[]{
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_SIZE
                },
                null,
                null,
                null
        )) {
            return c != null && c.moveToFirst();
        } catch (Exception ignored) {
            try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                return in != null;
            } catch (Exception ignoredAgain) {
                return false;
            }
        }
    }

    public static String getDisplayName(Context context, Uri uri) {
        if (uri == null) return "";

        if ("file".equalsIgnoreCase(uri.getScheme())) {
            String path = uri.getPath();
            return path == null ? "" : new File(path).getName();
        }

        try (Cursor c = context.getContentResolver().query(
                uri,
                new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME},
                null,
                null,
                null
        )) {
            if (c != null && c.moveToFirst()) return c.getString(0);
        } catch (Exception ignored) {
        }
        return "";
    }

    public static String titleFromFileName(String fileName) {
        if (fileName == null) return "Vídeo";
        String base = fileName;
        int dot = base.lastIndexOf('.');
        if (dot > 0) base = base.substring(0, dot);
        base = base.replace('_', ' ').replaceAll("\\s+", " ").trim();
        base = base.replaceFirst("^[A-Z]{0,3}[0-9]\\s*-\\s*", "").trim();
        return base.isEmpty() ? "Vídeo" : base;
    }

    public static String extensionOf(String fileName) {
        if (fileName == null) return "mp4";
        int dot = fileName.lastIndexOf('.');
        if (dot >= 0 && dot < fileName.length() - 1) return fileName.substring(dot + 1).toLowerCase();
        return "mp4";
    }

    public static String safeFileName(String code, String title, String extension) {
        String clean = title == null ? "Vídeo" : title;
        clean = clean.replaceAll("[\\\\/:*?\"<>|]", " ");
        clean = clean.replaceAll("\\s+", " ").trim();
        if (clean.isEmpty()) clean = "Vídeo";
        if (clean.length() > 80) clean = clean.substring(0, 80).trim();
        return code + " - " + clean + "." + extension;
    }

    private static File getPrivateTrashDir(Context context) {
        File base = context.getExternalFilesDir(null);
        if (base == null) base = context.getFilesDir();

        File trash = new File(base, PRIVATE_TRASH_DIR_NAME);
        if (!trash.exists() && !trash.mkdirs()) {
            throw new IllegalStateException("Não foi possível criar a lixeira privada");
        }
        return trash;
    }

    private static File uniqueFile(File parent, String requestedName) {
        String name = requestedName == null || requestedName.isBlank() ? "video.mp4" : requestedName;
        File file = new File(parent, name);
        if (!file.exists()) return file;

        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot) : "";

        int i = 2;
        while (file.exists()) {
            file = new File(parent, base + " (" + i + ")" + ext);
            i++;
        }
        return file;
    }

    private static void copyFromContentUri(ContentResolver resolver, Uri source, File target) throws Exception {
        boolean copied = false;
        try (InputStream in = resolver.openInputStream(source);
             OutputStream out = new FileOutputStream(target)) {
            if (in == null) throw new IllegalStateException("Não foi possível abrir o vídeo");
            copy(in, out);
            copied = true;
        } finally {
            if (!copied) target.delete();
        }
    }

    private static void copy(InputStream in, OutputStream out) throws Exception {
        byte[] buffer = new byte[1024 * 256];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        out.flush();
    }

    private static String mimeForName(String fileName) {
        String ext = extensionOf(fileName);
        if ("mkv".equals(ext)) return "video/x-matroska";
        if ("webm".equals(ext)) return "video/webm";
        if ("mov".equals(ext)) return "video/quicktime";
        if ("m4v".equals(ext)) return "video/x-m4v";
        return "video/mp4";
    }

    private static Uri requireTree(Context context) {
        Uri tree = getTreeUri(context);
        if (tree == null) throw new IllegalStateException("Pasta não conectada");
        return tree;
    }

    private static Uri rootDocumentUri(Uri tree) {
        return DocumentsContract.buildDocumentUriUsingTree(
                tree,
                DocumentsContract.getTreeDocumentId(tree)
        );
    }

    private static Uri findDirectory(Context context, Uri tree, Uri parent, String name) {
        try {
            String parentId = DocumentsContract.getDocumentId(parent);
            Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId);

            try (Cursor c = context.getContentResolver().query(
                    children,
                    new String[]{
                            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                            DocumentsContract.Document.COLUMN_MIME_TYPE
                    },
                    null,
                    null,
                    null
            )) {
                if (c != null) {
                    while (c.moveToNext()) {
                        if (name.equals(c.getString(1)) &&
                                DocumentsContract.Document.MIME_TYPE_DIR.equals(c.getString(2))) {
                            return DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0));
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static Uri moveWithFallback(
            Context context,
            Uri source,
            Uri sourceParent,
            Uri targetParent,
            String fileName
    ) throws Exception {
        ContentResolver resolver = context.getContentResolver();

        try {
            Uri moved = DocumentsContract.moveDocument(resolver, source, sourceParent, targetParent);
            if (moved != null) return moved;
        } catch (Exception ignored) {
        }

        String mime = resolver.getType(source);
        if (mime == null || mime.isEmpty()) mime = mimeForName(fileName);

        Uri target = DocumentsContract.createDocument(resolver, targetParent, mime, fileName);
        if (target == null) throw new IllegalStateException("Não foi possível mover o arquivo");

        boolean copied = false;
        try (InputStream in = resolver.openInputStream(source);
             OutputStream out = resolver.openOutputStream(target, "w")) {
            if (in == null || out == null) throw new IllegalStateException("Não foi possível copiar o arquivo");
            copy(in, out);
            copied = true;
        }

        if (!copied || !DocumentsContract.deleteDocument(resolver, source)) {
            try {
                DocumentsContract.deleteDocument(resolver, target);
            } catch (Exception ignored) {
            }
            throw new IllegalStateException("Não foi possível concluir a movimentação");
        }

        return target;
    }

    private static boolean isVideoName(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase();
        return lower.endsWith(".mp4") ||
                lower.endsWith(".mkv") ||
                lower.endsWith(".webm") ||
                lower.endsWith(".mov") ||
                lower.endsWith(".m4v");
    }

    public static class Entry {
        public final Uri uri;
        public final String name;
        public final String mimeType;
        public final long lastModified;
        public final long size;

        public Entry(Uri uri, String name, String mimeType, long lastModified, long size) {
            this.uri = uri;
            this.name = name;
            this.mimeType = mimeType;
            this.lastModified = lastModified;
            this.size = size;
        }
    }
}
