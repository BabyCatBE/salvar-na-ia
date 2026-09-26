package com.babycatbe.salvarnaia;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

public class ModelDownloadReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null ||
                !DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
            return;
        }

        long completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
        if (completedId != LocalAiModelManager.getSavedDownloadId(context)) return;

        if (LocalAiModelManager.isModelDownloaded(context)) {
            Toast.makeText(
                    context,
                    "Modelo da IA local baixado ✓",
                    Toast.LENGTH_LONG
            ).show();

            Intent service = new Intent(context, DownloadMonitorService.class);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(service);
                } else {
                    context.startService(service);
                }
            } catch (Exception ignored) {
                // If Android blocks a background foreground-service start,
                // MainActivity resumes the pending analysis next time it opens.
            }
        } else {
            Toast.makeText(
                    context,
                    "O download do modelo da IA local não foi concluído",
                    Toast.LENGTH_LONG
            ).show();
        }
    }
}
