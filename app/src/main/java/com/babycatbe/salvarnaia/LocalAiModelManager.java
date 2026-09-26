package com.babycatbe.salvarnaia;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.Locale;

public final class LocalAiModelManager {

    public static final String MODEL_NAME = "Gemma 4 E2B";
    public static final String MODEL_FILE_NAME = "gemma-4-E2B-it.litertlm";
    public static final long MODEL_SIZE_BYTES = 2_588_147_712L;
    public static final String MODEL_SHA256 =
            "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c";

    private static final String MODEL_URL =
            "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/" +
                    "6e5c4f1e395deb959c494953478fa5cec4b8008f/" +
                    MODEL_FILE_NAME + "?download=true";

    private static final String PREFS = "local_ai_model";
    private static final String KEY_DOWNLOAD_ID = "download_id";
    private static final String KEY_VERIFIED_SHA = "verified_sha";

    private LocalAiModelManager() {
    }

    public static File getModelFile(Context context) {
        File base = context.getExternalFilesDir(null);
        if (base == null) base = context.getFilesDir();

        File dir = new File(base, "models");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, MODEL_FILE_NAME);
    }

    public static boolean isModelDownloaded(Context context) {
        File file = getModelFile(context);
        return file.isFile() && file.length() == MODEL_SIZE_BYTES;
    }

    public static boolean isModelReady(Context context) {
        if (!isModelDownloaded(context)) return false;
        String verified = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_VERIFIED_SHA, "");
        return MODEL_SHA256.equalsIgnoreCase(verified);
    }

    public static synchronized boolean ensureVerified(Context context) {
        if (isModelReady(context)) return true;

        File model = getModelFile(context);
        if (!model.isFile() || model.length() != MODEL_SIZE_BYTES) return false;

        try {
            String actual = sha256(model);
            if (!MODEL_SHA256.equalsIgnoreCase(actual)) {
                model.delete();
                clearVerification(context);
                return false;
            }

            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_VERIFIED_SHA, MODEL_SHA256)
                    .apply();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static synchronized long startDownload(Context context) {
        if (isModelDownloaded(context)) return -1L;

        File external = context.getExternalFilesDir(null);
        if (external == null) {
            throw new IllegalStateException("Armazenamento do app indisponível");
        }

        DownloadManager manager =
                (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

        long current = getSavedDownloadId(context);
        DownloadState state = getDownloadState(context);
        if (current > 0 && state.isActive()) return current;

        if (current > 0) {
            try {
                manager.remove(current);
            } catch (Exception ignored) {
            }
        }

        File model = getModelFile(context);
        if (model.exists()) model.delete();
        clearVerification(context);

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(MODEL_URL))
                .setTitle("IA local do Salvar na IA")
                .setDescription("Baixando " + MODEL_NAME + " (~2,6 GB)")
                .setNotificationVisibility(
                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                .setAllowedOverRoaming(false)
                .setDestinationInExternalFilesDir(
                        context,
                        null,
                        "models/" + MODEL_FILE_NAME
                );

        long id = manager.enqueue(request);
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_DOWNLOAD_ID, id)
                .apply();
        return id;
    }

    public static long getSavedDownloadId(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getLong(KEY_DOWNLOAD_ID, -1L);
    }

    public static DownloadState getDownloadState(Context context) {
        long id = getSavedDownloadId(context);
        if (id <= 0) return DownloadState.none();

        DownloadManager manager =
                (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(id);

        try (Cursor c = manager.query(query)) {
            if (c == null || !c.moveToFirst()) return DownloadState.none();

            int status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
            long downloaded = c.getLong(
                    c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            );
            long total = c.getLong(
                    c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            );
            int reason = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON));
            return new DownloadState(status, downloaded, total, reason);
        } catch (Exception ignored) {
            return DownloadState.none();
        }
    }

    public static String humanSize() {
        return String.format(Locale.getDefault(), "%.2f GB", MODEL_SIZE_BYTES / 1_000_000_000.0);
    }

    private static void clearVerification(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_VERIFIED_SHA)
                .apply();
    }

    private static String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[1024 * 1024];

        try (FileInputStream in = new FileInputStream(file)) {
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }

        StringBuilder out = new StringBuilder();
        for (byte b : digest.digest()) {
            out.append(String.format(Locale.US, "%02x", b & 0xff));
        }
        return out.toString();
    }

    public static final class DownloadState {
        public final int status;
        public final long downloadedBytes;
        public final long totalBytes;
        public final int reason;

        DownloadState(int status, long downloadedBytes, long totalBytes, int reason) {
            this.status = status;
            this.downloadedBytes = downloadedBytes;
            this.totalBytes = totalBytes;
            this.reason = reason;
        }

        static DownloadState none() {
            return new DownloadState(0, 0, 0, 0);
        }

        public boolean isActive() {
            return status == DownloadManager.STATUS_PENDING ||
                    status == DownloadManager.STATUS_RUNNING ||
                    status == DownloadManager.STATUS_PAUSED;
        }

        public boolean isFailed() {
            return status == DownloadManager.STATUS_FAILED;
        }

        public int percent() {
            if (totalBytes <= 0 || downloadedBytes < 0) return -1;
            return (int) Math.min(100, (downloadedBytes * 100L) / totalBytes);
        }
    }
}
