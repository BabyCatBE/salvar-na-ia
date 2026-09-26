package com.babycatbe.salvarnaia;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Toast;

import java.util.regex.Matcher;

public class ShareReceiverActivity extends Activity {

    public static final String PREFS_NAME = "salvar_na_ia";
    public static final String KEY_LAST_RAW = "last_raw";
    public static final String KEY_LAST_URL = "last_url";
    public static final String KEY_LAST_RECEIVED_AT = "last_received_at";
    public static final String KEY_RECEIVED_COUNT = "received_count";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleShare(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleShare(intent);
    }

    private void handleShare(Intent intent) {
        if (intent == null || !Intent.ACTION_SEND.equals(intent.getAction())) {
            finishAndRemoveTask();
            return;
        }

        CharSequence shared = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);

        if ((shared == null || shared.length() == 0) && intent.getClipData() != null) {
            ClipData clipData = intent.getClipData();
            if (clipData.getItemCount() > 0) {
                shared = clipData.getItemAt(0).coerceToText(this);
            }
        }

        if (shared == null || shared.toString().trim().isEmpty()) {
            Toast.makeText(this, "Nenhum link recebido", Toast.LENGTH_SHORT).show();
            finishAndRemoveTask();
            return;
        }

        String rawText = shared.toString().trim();
        String detectedUrl = findFirstUrl(rawText);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int newCount = prefs.getInt(KEY_RECEIVED_COUNT, 0) + 1;
        prefs.edit()
                .putString(KEY_LAST_RAW, rawText)
                .putString(KEY_LAST_URL, detectedUrl)
                .putLong(KEY_LAST_RECEIVED_AT, System.currentTimeMillis())
                .putInt(KEY_RECEIVED_COUNT, newCount)
                .apply();

        AppStore store = new AppStore(this);
        AppStore.Item item = store.addDownloading(detectedUrl);

        if (!FolderManager.hasFolderAccess(this)) {
            store.markError(item.id, "Conecte a pasta Download_Videos IA no app");
            Toast.makeText(this, "Abra Salvar na IA e conecte a pasta", Toast.LENGTH_LONG).show();
            finishAndRemoveTask();
            return;
        }

        try {
            YtdlnisHelper.sendCommandDownload(this, detectedUrl);
            startMonitorService();
            Toast.makeText(this, "Enviado ao YTDLnis ✓", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            store.markError(item.id, "Não foi possível abrir o YTDLnis");
            Toast.makeText(this, "Não foi possível abrir o YTDLnis", Toast.LENGTH_LONG).show();
        }

        finishAndRemoveTask();
    }

    private void startMonitorService() {
        Intent service = new Intent(this, DownloadMonitorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(service);
        } else {
            startService(service);
        }
    }

    private String findFirstUrl(String text) {
        Matcher matcher = Patterns.WEB_URL.matcher(text);
        if (matcher.find()) return matcher.group();
        return text;
    }
}
