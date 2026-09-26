package com.babycatbe.salvarnaia;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class DownloadMonitorService extends Service {

    private static final String CHANNEL_ID = "salvar_na_ia_downloads";
    private static final int NOTIFICATION_ID = 41;
    private static final long TIMEOUT_MS = 15L * 60L * 1000L;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, buildNotification("Acompanhando downloads…"));

        if (running.compareAndSet(false, true)) {
            executor.execute(this::monitorLoop);
        }

        return START_STICKY;
    }

    private void monitorLoop() {
        AppStore store = new AppStore(this);

        try {
            while (true) {
                List<AppStore.Item> downloading = store.getDownloading();
                if (downloading.isEmpty()) break;

                if (!FolderManager.hasFolderAccess(this)) {
                    for (AppStore.Item item : downloading) {
                        store.markError(item.id, "Acesso à pasta foi perdido");
                    }
                    break;
                }

                List<FolderManager.Entry> files = FolderManager.listRootVideos(this);
                Set<String> used = new HashSet<>(store.getUsedFileUris());

                for (AppStore.Item item : downloading) {
                    FolderManager.Entry candidate = null;

                    for (FolderManager.Entry file : files) {
                        String key = file.uri.toString();
                        if (used.contains(key)) continue;
                        if (file.lastModified + 5000L < item.updatedAt) continue;
                        candidate = file;
                        break;
                    }

                    if (candidate != null) {
                        String title = FolderManager.titleFromFileName(candidate.name);
                        String ext = FolderManager.extensionOf(candidate.name);
                        String wantedName = FolderManager.safeFileName(item.code, title, ext);
                        Uri finalUri = FolderManager.rename(this, candidate.uri, wantedName);
                        String actualName = FolderManager.getDisplayName(this, finalUri);
                        if (actualName == null || actualName.isEmpty()) actualName = wantedName;

                        store.markReady(
                                item.id,
                                finalUri.toString(),
                                actualName,
                                title
                        );

                        used.add(finalUri.toString());
                    } else if (System.currentTimeMillis() - item.updatedAt > TIMEOUT_MS) {
                        store.markError(item.id, "O download não apareceu na pasta em até 15 minutos");
                    }
                }

                int left = store.getDownloading().size();
                updateNotification(left == 0
                        ? "Downloads finalizados"
                        : (left == 1 ? "1 vídeo baixando…" : left + " vídeos baixando…"));

                if (left == 0) break;

                try {
                    Thread.sleep(2500L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } finally {
            running.set(false);
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
        }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Downloads do Salvar na IA",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Acompanha os vídeos enviados ao YTDLnis");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(String text) {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(
                this,
                0,
                open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        return builder
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Salvar na IA")
                .setContentText(text)
                .setContentIntent(pending)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();
    }

    private void updateNotification(String text) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, buildNotification(text));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
