package com.babycatbe.salvarnaia;

import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class AppUpdateDownloadReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "salvar_na_ia_updates";
    private static final int NOTIFICATION_ID = 74;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null ||
                !DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
            return;
        }

        long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
        if (!AppUpdateManager.isSavedDownload(context, id)) return;
        if (!AppUpdateManager.isDownloaded(context)) return;

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Atualizações do Salvar na IA",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Avisa quando uma nova versão está pronta para instalar");
            manager.createNotificationChannel(channel);
        }

        Intent open = new Intent(context, MainActivity.class);
        open.setAction(MainActivity.ACTION_INSTALL_UPDATE);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pending = PendingIntent.getActivity(
                context,
                74,
                open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);

        Notification notification = builder
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Atualização pronta")
                .setContentText(
                        "Salvar na IA v" +
                                AppUpdateManager.getSavedTargetVersion(context) +
                                " • toque para instalar"
                )
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build();

        manager.notify(NOTIFICATION_ID, notification);
    }
}
