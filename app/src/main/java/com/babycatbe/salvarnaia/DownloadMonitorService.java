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
        startForeground(NOTIFICATION_ID, buildNotification("Acompanhando vídeos…"));

        if (running.compareAndSet(false, true)) {
            executor.execute(this::monitorLoop);
        }

        return START_STICKY;
    }

    private void monitorLoop() {
        AppStore store = new AppStore(this);
        LocalAiEngine aiEngine = null;

        try {
            while (true) {
                List<AppStore.Item> downloading = store.getDownloading();

                if (!downloading.isEmpty()) {
                    if (!FolderManager.hasFolderAccess(this)) {
                        for (AppStore.Item item : downloading) {
                            store.markError(item.id, "Acesso à pasta foi perdido");
                        }
                    } else {
                        processDownloads(store, downloading);
                    }
                }

                List<AppStore.Item> waitingForAi = store.getAiSetupRequired();
                if (!waitingForAi.isEmpty() &&
                        LocalAiModelManager.isModelDownloaded(this)) {
                    for (AppStore.Item item : waitingForAi) {
                        if (item.fileUri == null ||
                                item.fileUri.isEmpty() ||
                                !FolderManager.exists(this, Uri.parse(item.fileUri))) {
                            store.markFileMissing(item.id);
                        } else {
                            store.markAnalyzing(item.id);
                        }
                    }
                }

                List<AppStore.Item> analyzing = store.getAnalyzing();
                if (!analyzing.isEmpty()) {
                    if (!LocalAiModelManager.isModelDownloaded(this)) {
                        for (AppStore.Item item : analyzing) {
                            store.markAiSetupRequired(
                                    item.id,
                                    item.fileUri,
                                    item.fileName,
                                    item.title
                            );
                        }
                    } else if (!LocalAiModelManager.ensureVerified(this)) {
                        for (AppStore.Item item : analyzing) {
                            store.markAnalysisError(
                                    item.id,
                                    "O modelo da IA local está incompleto ou inválido. Baixe novamente."
                            );
                        }
                    } else {
                        updateNotification("Preparando IA local…");

                        if (aiEngine == null) {
                            try {
                                aiEngine = new LocalAiEngine(this);
                            } catch (Exception e) {
                                for (AppStore.Item item : analyzing) {
                                    store.markAnalysisError(
                                            item.id,
                                            "Não foi possível iniciar a IA local neste aparelho"
                                    );
                                }
                                analyzing.clear();
                            }
                        }

                        if (aiEngine != null) {
                            for (AppStore.Item item : analyzing) {
                                if (item.fileUri == null ||
                                        item.fileUri.isEmpty() ||
                                        !FolderManager.exists(this, Uri.parse(item.fileUri))) {
                                    store.markFileMissing(item.id);
                                    continue;
                                }

                                updateNotification(
                                        "Analisando vídeo " + item.code + " com IA local…"
                                );

                                try {
                                    LocalAiEngine.AnalysisResult result =
                                            aiEngine.analyze(
                                                    Uri.parse(item.fileUri),
                                                    item.title
                                            );
                                    store.markAnalysisReady(
                                            item.id,
                                            result.title,
                                            result.summary
                                    );
                                } catch (Exception e) {
                                    store.markAnalysisError(
                                            item.id,
                                            "A IA local não conseguiu concluir a análise"
                                    );
                                }
                            }
                        }
                    }
                }

                int downloadingLeft = store.getDownloading().size();
                int analyzingLeft = store.getAnalyzing().size();
                int setupLeft = store.getAiSetupRequired().size();

                if (downloadingLeft == 0 && analyzingLeft == 0) {
                    if (setupLeft > 0 &&
                            !LocalAiModelManager.isModelDownloaded(this)) {
                        updateNotification("Modelo da IA local precisa ser baixado");
                    } else {
                        updateNotification("Vídeos finalizados");
                    }
                    break;
                }

                updateNotification(
                        analyzingLeft > 0
                                ? "Analisando com IA local…"
                                : (downloadingLeft == 1
                                ? "1 vídeo baixando…"
                                : downloadingLeft + " vídeos baixando…")
                );

                if (downloadingLeft > 0) {
                    try {
                        Thread.sleep(2500L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        } finally {
            if (aiEngine != null) {
                try {
                    aiEngine.close();
                } catch (Exception ignored) {
                }
            }
            running.set(false);
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
        }
    }

    private void processDownloads(
            AppStore store,
            List<AppStore.Item> downloading
    ) {
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

                if (LocalAiModelManager.isModelDownloaded(this)) {
                    store.markAnalyzing(
                            item.id,
                            finalUri.toString(),
                            actualName,
                            title
                    );
                } else {
                    store.markAiSetupRequired(
                            item.id,
                            finalUri.toString(),
                            actualName,
                            title
                    );
                }

                used.add(finalUri.toString());
            } else if (System.currentTimeMillis() - item.updatedAt > TIMEOUT_MS) {
                store.markError(
                        item.id,
                        "O download não apareceu na pasta em até 15 minutos"
                );
            }
        }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Downloads e IA local",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Acompanha downloads e a pré-análise local dos vídeos");
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
        NotificationManager manager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
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
