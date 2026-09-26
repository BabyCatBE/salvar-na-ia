package com.babycatbe.salvarnaia;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FolderManager {

    public static final String PREFS = "salvar_na_ia";
    public static final String KEY_TREE_URI = "videos_tree_uri";
    public static final String TRASH_DIR_NAME = "Lixeira Salvar na IA";

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

    public static Uri rename(Context context, Uri uri, String newName) {
        try {
            Uri renamed = DocumentsContract.renameDocument(context.getContentResolver(), uri, newName);
            return renamed == null ? uri : renamed;
        } catch (Exception ignored) {
            return uri;
        }
    }

    public static Uri moveToTrash(Context context, Uri fileUri, String fileName) throws Exception {
        Uri tree = requireTree(context);
        Uri root = rootDocumentUri(tree);
        Uri trash = getOrCreateDirectory(context, tree, root, TRASH_DIR_NAME);
        return moveWithFallback(context, fileUri, root, trash, fileName);
    }

    public static Uri restoreFromTrash(Context context, Uri fileUri, String fileName) throws Exception {
        Uri tree = requireTree(context);
        Uri root = rootDocumentUri(tree);
        Uri trash = getOrCreateDirectory(context, tree, root, TRASH_DIR_NAME);
        return moveWithFallback(context, fileUri, trash, root, fileName);
    }

    public static boolean delete(Context context, Uri uri) {
        try {
            return DocumentsContract.deleteDocument(context.getContentResolver(), uri);
        } catch (Exception ignored) {
            return false;
        }
    }

    public static String getDisplayName(Context context, Uri uri) {
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

    private static Uri getOrCreateDirectory(Context context, Uri tree, Uri parent, String name) throws Exception {
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

        Uri created = DocumentsContract.createDocument(
                context.getContentResolver(),
                parent,
                DocumentsContract.Document.MIME_TYPE_DIR,
                name
        );
        if (created == null) throw new IllegalStateException("Não foi possível criar a lixeira");
        return created;
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
        if (mime == null || mime.isEmpty()) mime = "application/octet-stream";

        Uri target = DocumentsContract.createDocument(resolver, targetParent, mime, fileName);
        if (target == null) throw new IllegalStateException("Não foi possível mover o arquivo");

        try (InputStream in = resolver.openInputStream(source);
             OutputStream out = resolver.openOutputStream(target, "w")) {
            if (in == null || out == null) throw new IllegalStateException("Não foi possível copiar o arquivo");
            byte[] buffer = new byte[1024 * 256];
            int read;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            out.flush();
        }

        DocumentsContract.deleteDocument(resolver, source);
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
