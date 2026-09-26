package com.babycatbe.salvarnaia;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class AppUpdateManager {

    private static final String RELEASES_API =
            "https://api.github.com/repos/BabyCatBE/salvar-na-ia/releases/latest";

    private static final String PREFS = "salvar_na_ia_updates";
    private static final String KEY_DOWNLOAD_ID = "download_id";
    private static final String KEY_TARGET_VERSION = "target_version";
    private static final String KEY_TARGET_TAG = "target_tag";
    private static final String KEY_AWAITING_PERMISSION = "awaiting_install_permission";

    private AppUpdateManager() {
    }

    public static UpdateInfo checkLatest(Context context) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(RELEASES_API).openConnection();
            connection.setConnectTimeout(15_000);
            connection.setReadTimeout(20_000);
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("User-Agent", "Salvar-na-IA-Android");
            connection.setInstanceFollowRedirects(true);

            int code = connection.getResponseCode();
            if (code != 200) {
                throw new IllegalStateException("GitHub respondeu HTTP " + code);
            }

            String body = readAll(connection.getInputStream());
            JSONObject release = new JSONObject(body);

            String tag = release.optString("tag_name", "");
            String version = normalizeVersion(tag);
            if (version.isEmpty()) {
                throw new IllegalStateException("Release sem versão válida");
            }

            JSONArray assets = release.optJSONArray("assets");
            String apkUrl = "";
            String apkName = "";

            if (assets != null) {
                for (int i = 0; i < assets.length(); i++) {
                    JSONObject asset = assets.optJSONObject(i);
                    if (asset == null) continue;

                    String name = asset.optString("name", "");
                    String url = asset.optString("browser_download_url", "");
                    if (name.toLowerCase().endsWith(".apk") && !url.isEmpty()) {
                        apkName = name;
                        apkUrl = url;
                        break;
                    }
                }
            }

            if (apkUrl.isEmpty()) {
                throw new IllegalStateException("A release mais recente não contém APK");
            }

            String current = getCurrentVersion(context);
            return new UpdateInfo(
                    version,
                    tag,
                    apkUrl,
                    apkName,
                    compareVersions(version, current) > 0
            );
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    public static long startDownload(Context context, UpdateInfo info) {
        if (info == null || info.apkUrl == null || info.apkUrl.isEmpty()) {
            throw new IllegalArgumentException("Atualização inválida");
        }

        DownloadManager manager =
                (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

        long oldId = getSavedDownloadId(context);
        if (oldId > 0) {
            try {
                manager.remove(oldId);
            } catch (Exception ignored) {
            }
        }

        String fileName = "Salvar-na-IA-v" + info.version + ".apk";

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(info.apkUrl))
                .setTitle("Salvar na IA v" + info.version)
                .setDescription("Baixando atualização")
                .setMimeType("application/vnd.android.package-archive")
                .setNotificationVisibility(
                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                .setAllowedOverRoaming(false)
                .setDestinationInExternalFilesDir(
                        context,
                        Environment.DIRECTORY_DOWNLOADS,
                        fileName
                );

        long id = manager.enqueue(request);
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_DOWNLOAD_ID, id)
                .putString(KEY_TARGET_VERSION, info.version)
                .putString(KEY_TARGET_TAG, info.tag)
                .putBoolean(KEY_AWAITING_PERMISSION, false)
                .apply();

        return id;
    }

    public static long getSavedDownloadId(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getLong(KEY_DOWNLOAD_ID, -1L);
    }

    public static String getSavedTargetVersion(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_TARGET_VERSION, "");
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

    public static boolean isDownloaded(Context context) {
        return getDownloadState(context).isSuccessful();
    }

    public static boolean installDownloadedUpdate(Activity activity) {
        long id = getSavedDownloadId(activity);
        if (id <= 0) return false;

        DownloadManager manager =
                (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = manager.getUriForDownloadedFile(id);
        if (uri == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !activity.getPackageManager().canRequestPackageInstalls()) {
            activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(KEY_AWAITING_PERMISSION, true)
                    .apply();

            Intent permission = new Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + activity.getPackageName())
            );
            activity.startActivity(permission);
            return false;
        }

        activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_AWAITING_PERMISSION, false)
                .apply();

        Intent install = new Intent(Intent.ACTION_VIEW);
        install.setDataAndType(uri, "application/vnd.android.package-archive");
        install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(install);
        return true;
    }

    public static boolean resumeInstallAfterPermission(Activity activity) {
        SharedPreferences prefs =
                activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        boolean awaiting = prefs.getBoolean(KEY_AWAITING_PERMISSION, false);
        if (!awaiting) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !activity.getPackageManager().canRequestPackageInstalls()) {
            return false;
        }

        prefs.edit().putBoolean(KEY_AWAITING_PERMISSION, false).apply();
        return installDownloadedUpdate(activity);
    }

    public static void cleanupAfterInstalledUpdate(Context context) {
        String target = getSavedTargetVersion(context);
        if (target == null || target.isEmpty()) return;

        String current = getCurrentVersion(context);
        if (compareVersions(current, target) < 0) return;

        long id = getSavedDownloadId(context);
        if (id > 0) {
            try {
                DownloadManager manager =
                        (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                manager.remove(id);
            } catch (Exception ignored) {
            }
        }

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }

    public static boolean isSavedDownload(Context context, long id) {
        return id > 0 && id == getSavedDownloadId(context);
    }

    public static String getCurrentVersion(Context context) {
        try {
            PackageInfo info =
                    context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return info.versionName == null ? "0.0.0" : info.versionName;
        } catch (Exception ignored) {
            return "0.0.0";
        }
    }

    private static String normalizeVersion(String tag) {
        if (tag == null) return "";
        String value = tag.trim();
        if (value.startsWith("v") || value.startsWith("V")) {
            value = value.substring(1);
        }
        return value;
    }

    public static int compareVersions(String left, String right) {
        String[] a = normalizeVersion(left).split("\\.");
        String[] b = normalizeVersion(right).split("\\.");
        int length = Math.max(a.length, b.length);

        for (int i = 0; i < length; i++) {
            int av = i < a.length ? numericPart(a[i]) : 0;
            int bv = i < b.length ? numericPart(b[i]) : 0;
            if (av != bv) return Integer.compare(av, bv);
        }
        return 0;
    }

    private static int numericPart(String value) {
        if (value == null) return 0;
        String digits = value.replaceAll("[^0-9].*$", "");
        if (digits.isEmpty()) return 0;
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static String readAll(InputStream input) throws Exception {
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8)
        )) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }
        }
        return out.toString();
    }

    public static final class UpdateInfo {
        public final String version;
        public final String tag;
        public final String apkUrl;
        public final String apkName;
        public final boolean updateAvailable;

        UpdateInfo(
                String version,
                String tag,
                String apkUrl,
                String apkName,
                boolean updateAvailable
        ) {
            this.version = version;
            this.tag = tag;
            this.apkUrl = apkUrl;
            this.apkName = apkName;
            this.updateAvailable = updateAvailable;
        }
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

        public boolean isSuccessful() {
            return status == DownloadManager.STATUS_SUCCESSFUL;
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
