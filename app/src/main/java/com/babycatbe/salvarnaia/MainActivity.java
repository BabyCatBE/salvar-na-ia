package com.babycatbe.salvarnaia;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    public static final String ACTION_INSTALL_UPDATE =
            "com.babycatbe.salvarnaia.INSTALL_UPDATE";

    private static final int REQUEST_FOLDER = 3001;
    private static final int REQUEST_NOTIFICATIONS = 3002;
    private static final long TRASH_RETENTION_MS = 7L * 24L * 60L * 60L * 1000L;

    private static final int BG = Color.rgb(248, 246, 244);
    private static final int SURFACE = Color.WHITE;
    private static final int TEXT = Color.rgb(35, 31, 30);
    private static final int MUTED = Color.rgb(112, 103, 100);
    private static final int RED = Color.rgb(183, 28, 28);
    private static final int RED_SOFT = Color.rgb(255, 237, 235);
    private static final int BORDER = Color.rgb(232, 224, 221);
    private static final int GREEN = Color.rgb(38, 126, 71);
    private static final int GREEN_SOFT = Color.rgb(235, 247, 239);
    private static final int AMBER = Color.rgb(151, 100, 12);
    private static final int AMBER_SOFT = Color.rgb(255, 247, 224);

    private final ExecutorService io = Executors.newSingleThreadExecutor();

    private LinearLayout root;
    private LinearLayout listContainer;
    private TextView folderText;
    private TextView pendingTab;
    private TextView trashTab;
    private TextView aiStatusText;
    private TextView aiAction;
    private boolean showingTrash = false;
    private volatile boolean reconcilingMissingFiles = false;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Runnable aiDownloadPoller = new Runnable() {
        @Override
        public void run() {
            if (aiStatusText == null) return;
            updateAiCard();
            LocalAiModelManager.DownloadState state =
                    LocalAiModelManager.getDownloadState(MainActivity.this);
            if (state.isActive()) {
                uiHandler.postDelayed(this, 1500L);
            }
        }
    };

    private AppStore store;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new AppStore(this);
        AppUpdateManager.cleanupAfterInstalledUpdate(this);
        configureBars();
        buildUi();
        handleUpdateIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
        purgeExpiredTrashAsync();
        reconcileMissingFilesAsync();

        if (LocalAiModelManager.isModelDownloaded(this) &&
                !store.getAiSetupRequired().isEmpty()) {
            startMonitorService();
        }

        uiHandler.removeCallbacks(aiDownloadPoller);
        uiHandler.post(aiDownloadPoller);

        AppUpdateManager.resumeInstallAfterPermission(this);
    }

    @Override
    protected void onPause() {
        uiHandler.removeCallbacks(aiDownloadPoller);
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleUpdateIntent(intent);
    }

    private void handleUpdateIntent(Intent intent) {
        if (intent == null || !ACTION_INSTALL_UPDATE.equals(intent.getAction())) return;

        uiHandler.post(() -> {
            if (!AppUpdateManager.installDownloadedUpdate(this)) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                        getPackageManager().canRequestPackageInstalls()) {
                    Toast.makeText(
                            this,
                            "Não foi possível abrir o instalador da atualização",
                            Toast.LENGTH_LONG
                    ).show();
                }
            }
        });
    }

    private void configureBars() {
        Window window = getWindow();
        window.setStatusBarColor(BG);
        window.setNavigationBarColor(BG);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(28), dp(20), dp(36));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);

        TextView eyebrow = text("SALVAR NA IA", 12, true);
        eyebrow.setTextColor(RED);
        eyebrow.setLetterSpacing(0.12f);
        LinearLayout.LayoutParams eyebrowP = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
        topBar.addView(eyebrow, eyebrowP);

        TextView settingsButton = text("⚙", 20, false);
        settingsButton.setGravity(Gravity.CENTER);
        settingsButton.setPadding(dp(10), dp(6), dp(10), dp(6));
        settingsButton.setBackground(
                rounded(Color.rgb(239, 238, 237), BORDER, 99)
        );
        settingsButton.setClickable(true);
        settingsButton.setFocusable(true);
        settingsButton.setContentDescription("Configurações");
        settingsButton.setOnClickListener(v -> showSettings());
        topBar.addView(settingsButton, wrapWrap());

        root.addView(topBar);

        TextView title = text("Vídeos prontos para análise.", 29, true);
        title.setTextColor(TEXT);
        LinearLayout.LayoutParams titleP = matchWrap();
        titleP.topMargin = dp(8);
        root.addView(title, titleP);

        TextView subtitle = text(
                "Compartilhe um vídeo. O YTDLnis baixa; o Salvar na IA organiza.",
                15,
                false
        );
        subtitle.setTextColor(MUTED);
        subtitle.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams subP = matchWrap();
        subP.topMargin = dp(8);
        subP.bottomMargin = dp(18);
        root.addView(subtitle, subP);

        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.addView(chip("v" + getVersionName(), RED_SOFT, RED));
        TextView beta = chip("Beta integrada", Color.rgb(239, 238, 237), MUTED);
        LinearLayout.LayoutParams betaP = wrapWrap();
        betaP.leftMargin = dp(8);
        chips.addView(beta, betaP);
        LinearLayout.LayoutParams chipsP = matchWrap();
        chipsP.bottomMargin = dp(18);
        root.addView(chips, chipsP);

        LinearLayout folderCard = card();
        folderCard.addView(label("PASTA DOS VÍDEOS"));
        folderText = text("", 16, true);
        folderText.setTextColor(TEXT);
        LinearLayout.LayoutParams folderTextP = matchWrap();
        folderTextP.topMargin = dp(8);
        folderCard.addView(folderText, folderTextP);

        TextView folderHint = text(
                "Conecte uma vez a mesma pasta usada pelo perfil Salvar na IA no YTDLnis.",
                13,
                false
        );
        folderHint.setTextColor(MUTED);
        folderHint.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams hintP = matchWrap();
        hintP.topMargin = dp(6);
        folderCard.addView(folderHint, hintP);

        TextView connect = actionButton("Conectar / trocar pasta", false);
        connect.setOnClickListener(v -> chooseFolder());
        LinearLayout.LayoutParams connectP = matchWrap();
        connectP.topMargin = dp(14);
        folderCard.addView(connect, connectP);

        LinearLayout.LayoutParams folderCardP = matchWrap();
        folderCardP.bottomMargin = dp(16);
        root.addView(folderCard, folderCardP);

        LinearLayout aiCard = card();
        aiCard.addView(label("IA LOCAL"));

        aiStatusText = text("", 16, true);
        aiStatusText.setTextColor(TEXT);
        LinearLayout.LayoutParams aiStatusP = matchWrap();
        aiStatusP.topMargin = dp(8);
        aiCard.addView(aiStatusText, aiStatusP);

        TextView aiHint = text(
                "Todo vídeo novo será analisado automaticamente no aparelho. " +
                        "O modelo é baixado uma vez e depois funciona local/offline.",
                13,
                false
        );
        aiHint.setTextColor(MUTED);
        aiHint.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams aiHintP = matchWrap();
        aiHintP.topMargin = dp(6);
        aiCard.addView(aiHint, aiHintP);

        aiAction = actionButton("Baixar modelo da IA (~2,6 GB)", false);
        aiAction.setOnClickListener(v -> startModelDownload());
        LinearLayout.LayoutParams aiActionP = matchWrap();
        aiActionP.topMargin = dp(14);
        aiCard.addView(aiAction, aiActionP);

        LinearLayout.LayoutParams aiCardP = matchWrap();
        aiCardP.bottomMargin = dp(16);
        root.addView(aiCard, aiCardP);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setWeightSum(2f);

        pendingTab = tab("Pendentes", true);
        trashTab = tab("Lixeira", false);

        LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0, dp(46), 1f);
        left.rightMargin = dp(6);
        tabs.addView(pendingTab, left);

        LinearLayout.LayoutParams right = new LinearLayout.LayoutParams(0, dp(46), 1f);
        right.leftMargin = dp(6);
        tabs.addView(trashTab, right);

        pendingTab.setOnClickListener(v -> {
            showingTrash = false;
            updateTabs();
            renderList();
        });

        trashTab.setOnClickListener(v -> {
            showingTrash = true;
            updateTabs();
            renderList();
        });

        LinearLayout.LayoutParams tabsP = matchWrap();
        tabsP.bottomMargin = dp(14);
        root.addView(tabs, tabsP);

        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(listContainer, matchWrap());

        setContentView(scroll);
    }

    private void refresh() {
        if (FolderManager.hasFolderAccess(this)) {
            String name = FolderManager.getRootName(this);
            folderText.setText((name == null || name.isEmpty()) ? "Pasta conectada ✓" : name + " ✓");
            folderText.setTextColor(GREEN);
        } else {
            folderText.setText("Conecte Download_Videos IA");
            folderText.setTextColor(RED);
        }

        updateAiCard();
        updateTabs();
        renderList();
    }

    private void updateTabs() {
        pendingTab.setBackground(rounded(showingTrash ? Color.TRANSPARENT : RED_SOFT, showingTrash ? BORDER : RED_SOFT, 18));
        pendingTab.setTextColor(showingTrash ? MUTED : RED);
        trashTab.setBackground(rounded(showingTrash ? RED_SOFT : Color.TRANSPARENT, showingTrash ? RED_SOFT : BORDER, 18));
        trashTab.setTextColor(showingTrash ? RED : MUTED);
    }

    private void renderList() {
        listContainer.removeAllViews();

        List<AppStore.Item> items = showingTrash ? store.getTrash() : store.getPending();

        if (items.isEmpty()) {
            LinearLayout empty = card();
            TextView title = text(showingTrash ? "Lixeira vazia" : "Nada pendente", 18, true);
            title.setTextColor(TEXT);
            empty.addView(title);

            TextView desc = text(
                    showingTrash
                            ? "Itens enviados ficam aqui por até 7 dias."
                            : "Compartilhe um Reel ou TikTok com Salvar na IA.",
                    14,
                    false
            );
            desc.setTextColor(MUTED);
            LinearLayout.LayoutParams descP = matchWrap();
            descP.topMargin = dp(6);
            empty.addView(desc, descP);

            listContainer.addView(empty, cardSpacing());
            return;
        }

        if (showingTrash) {
            TextView emptyTrash = actionButton(
                    "Esvaziar lixeira (" + items.size() + ")",
                    false
            );
            emptyTrash.setTextColor(RED);
            emptyTrash.setOnClickListener(v -> confirmEmptyTrash());
            LinearLayout.LayoutParams emptyTrashP = matchWrap();
            emptyTrashP.bottomMargin = dp(12);
            listContainer.addView(emptyTrash, emptyTrashP);
        }

        for (AppStore.Item item : items) {
            listContainer.addView(
                    showingTrash ? trashCard(item) : pendingCard(item),
                    cardSpacing()
            );
        }
    }

    private View pendingCard(AppStore.Item item) {
        LinearLayout card = card();

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView code = chip(item.code, RED_SOFT, RED);
        top.addView(code);

        TextView status = statusChip(item.status);
        LinearLayout.LayoutParams statusP = wrapWrap();
        statusP.leftMargin = dp(8);
        top.addView(status, statusP);

        card.addView(top);

        String titleText = item.title == null || item.title.isEmpty()
                ? (AppStore.STATUS_DOWNLOADING.equals(item.status) ? "Baixando vídeo…" : "Vídeo")
                : item.title;

        TextView title = text(titleText, 19, true);
        title.setTextColor(TEXT);
        LinearLayout.LayoutParams titleP = matchWrap();
        titleP.topMargin = dp(12);
        card.addView(title, titleP);

        if (item.fileName != null && !item.fileName.isEmpty()) {
            TextView file = text(item.fileName, 13, false);
            file.setTextColor(MUTED);
            file.setTextIsSelectable(true);
            LinearLayout.LayoutParams fileP = matchWrap();
            fileP.topMargin = dp(6);
            card.addView(file, fileP);
        }

        if (item.summary != null && !item.summary.isEmpty()) {
            TextView summary = text(item.summary, 14, false);
            summary.setTextColor(TEXT);
            summary.setLineSpacing(dp(2), 1f);
            LinearLayout.LayoutParams summaryP = matchWrap();
            summaryP.topMargin = dp(10);
            card.addView(summary, summaryP);
        }

        TextView url = text(item.url, 12, false);
        url.setTextColor(MUTED);
        url.setTextIsSelectable(true);
        LinearLayout.LayoutParams urlP = matchWrap();
        urlP.topMargin = dp(8);
        card.addView(url, urlP);

        if (AppStore.STATUS_ERROR.equals(item.status) &&
                item.error != null &&
                !item.error.isEmpty()) {
            addErrorText(card, item.error);

            TextView retry = actionButton("Tentar download novamente", false);
            retry.setOnClickListener(v -> retry(item));
            LinearLayout.LayoutParams retryP = matchWrap();
            retryP.topMargin = dp(14);
            card.addView(retry, retryP);
        }

        if (AppStore.STATUS_FILE_MISSING.equals(item.status)) {
            addErrorText(
                    card,
                    "Arquivo ausente: o registro existe, mas o MP4 não foi encontrado."
            );

            TextView retry = actionButton("Baixar novamente", true);
            retry.setOnClickListener(v -> retry(item));
            LinearLayout.LayoutParams retryP = matchWrap();
            retryP.topMargin = dp(14);
            card.addView(retry, retryP);

            TextView remove = actionButton("Remover registro", false);
            remove.setTextColor(RED);
            remove.setOnClickListener(v -> confirmRemoveMissing(item));
            LinearLayout.LayoutParams removeP = matchWrap();
            removeP.topMargin = dp(8);
            card.addView(remove, removeP);
        }

        if (AppStore.STATUS_AI_SETUP_REQUIRED.equals(item.status)) {
            TextView info = text(
                    "O download terminou. Falta instalar o modelo da IA local uma única vez.",
                    13,
                    true
            );
            info.setTextColor(AMBER);
            LinearLayout.LayoutParams infoP = matchWrap();
            infoP.topMargin = dp(8);
            card.addView(info, infoP);

            if (!LocalAiModelManager.isModelDownloaded(this)) {
                TextView downloadAi = actionButton(
                        "Baixar modelo da IA (~2,6 GB)",
                        false
                );
                downloadAi.setOnClickListener(v -> startModelDownload());
                LinearLayout.LayoutParams downloadAiP = matchWrap();
                downloadAiP.topMargin = dp(14);
                card.addView(downloadAi, downloadAiP);
            }

            addSendOnly(card, item, "Mandar para análise mesmo assim");
        }

        if (AppStore.STATUS_ANALYSIS_ERROR.equals(item.status)) {
            addErrorText(
                    card,
                    item.error == null || item.error.isEmpty()
                            ? "A IA local não conseguiu concluir a análise"
                            : item.error
            );

            TextView retryAi = actionButton("Tentar análise novamente", false);
            retryAi.setOnClickListener(v -> retryAnalysis(item));
            LinearLayout.LayoutParams retryAiP = matchWrap();
            retryAiP.topMargin = dp(14);
            card.addView(retryAi, retryAiP);

            addSendOnly(card, item, "Mandar para análise mesmo assim");
        }

        if (AppStore.STATUS_READY.equals(item.status)) {
            addSendAndDone(card, item);
        }

        return card;
    }

    private View trashCard(AppStore.Item item) {
        LinearLayout card = card();

        TextView code = chip(item.code, Color.rgb(239, 238, 237), MUTED);
        card.addView(code);

        TextView title = text(
                item.title == null || item.title.isEmpty() ? "Vídeo enviado" : item.title,
                18,
                true
        );
        title.setTextColor(TEXT);
        LinearLayout.LayoutParams titleP = matchWrap();
        titleP.topMargin = dp(10);
        card.addView(title, titleP);

        long remaining = Math.max(0, (item.trashAt + TRASH_RETENTION_MS) - System.currentTimeMillis());
        long days = (remaining + (24L * 60L * 60L * 1000L) - 1) / (24L * 60L * 60L * 1000L);

        TextView info = text("Exclusão automática em até " + days + " dia(s)", 13, false);
        info.setTextColor(MUTED);
        LinearLayout.LayoutParams infoP = matchWrap();
        infoP.topMargin = dp(6);
        card.addView(info, infoP);

        TextView restore = actionButton("Restaurar", false);
        restore.setOnClickListener(v -> restore(item));
        LinearLayout.LayoutParams restoreP = matchWrap();
        restoreP.topMargin = dp(14);
        card.addView(restore, restoreP);

        TextView delete = actionButton("Excluir agora", false);
        delete.setTextColor(RED);
        delete.setOnClickListener(v -> deleteNow(item));
        LinearLayout.LayoutParams deleteP = matchWrap();
        deleteP.topMargin = dp(8);
        card.addView(delete, deleteP);

        return card;
    }

    private TextView statusChip(String status) {
        if (AppStore.STATUS_READY.equals(status)) {
            return chip("Pronto", GREEN_SOFT, GREEN);
        }
        if (AppStore.STATUS_ANALYZING.equals(status)) {
            return chip("Analisando IA", AMBER_SOFT, AMBER);
        }
        if (AppStore.STATUS_AI_SETUP_REQUIRED.equals(status)) {
            return chip("Aguardando IA", AMBER_SOFT, AMBER);
        }
        if (AppStore.STATUS_FILE_MISSING.equals(status)) {
            return chip("Arquivo ausente", RED_SOFT, RED);
        }
        if (AppStore.STATUS_ANALYSIS_ERROR.equals(status)) {
            return chip("Falha na análise", RED_SOFT, RED);
        }
        if (AppStore.STATUS_ERROR.equals(status)) {
            return chip("Erro", RED_SOFT, RED);
        }
        return chip("Baixando", AMBER_SOFT, AMBER);
    }

    private void updateAiCard() {
        if (aiStatusText == null || aiAction == null) return;

        if (LocalAiModelManager.isModelReady(this)) {
            aiStatusText.setText(LocalAiModelManager.MODEL_NAME + " ✓ • local/offline");
            aiStatusText.setTextColor(GREEN);
            aiAction.setVisibility(View.GONE);
            return;
        }

        if (LocalAiModelManager.isModelDownloaded(this)) {
            aiStatusText.setText(
                    LocalAiModelManager.MODEL_NAME +
                            " baixado • será verificado no primeiro uso"
            );
            aiStatusText.setTextColor(AMBER);
            aiAction.setVisibility(View.GONE);
            return;
        }

        LocalAiModelManager.DownloadState state =
                LocalAiModelManager.getDownloadState(this);

        if (state.isActive()) {
            int percent = state.percent();
            aiStatusText.setText(
                    percent >= 0
                            ? "Baixando " + LocalAiModelManager.MODEL_NAME +
                            " • " + percent + "%"
                            : "Baixando " + LocalAiModelManager.MODEL_NAME + "…"
            );
            aiStatusText.setTextColor(AMBER);
            aiAction.setVisibility(View.GONE);
            return;
        }

        if (state.isFailed()) {
            aiStatusText.setText("Falha ao baixar o modelo da IA local");
            aiStatusText.setTextColor(RED);
            aiAction.setText("Tentar baixar novamente (~2,6 GB)");
            aiAction.setVisibility(View.VISIBLE);
            return;
        }

        aiStatusText.setText(
                "Modelo não instalado • " + LocalAiModelManager.humanSize()
        );
        aiStatusText.setTextColor(MUTED);
        aiAction.setText("Baixar modelo da IA (~2,6 GB)");
        aiAction.setVisibility(View.VISIBLE);
    }

    private void startModelDownload() {
        try {
            if (LocalAiModelManager.isModelDownloaded(this)) {
                Toast.makeText(
                        this,
                        "O modelo já foi baixado",
                        Toast.LENGTH_SHORT
                ).show();
                startMonitorService();
                updateAiCard();
                return;
            }

            LocalAiModelManager.startDownload(this);
            Toast.makeText(
                    this,
                    "Download da IA local iniciado • ~2,6 GB",
                    Toast.LENGTH_LONG
            ).show();

            updateAiCard();
            uiHandler.removeCallbacks(aiDownloadPoller);
            uiHandler.post(aiDownloadPoller);
        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Não foi possível iniciar o download do modelo",
                    Toast.LENGTH_LONG
            ).show();
            updateAiCard();
        }
    }

    private void addErrorText(LinearLayout card, String message) {
        TextView error = text(message, 13, true);
        error.setTextColor(RED);
        LinearLayout.LayoutParams errP = matchWrap();
        errP.topMargin = dp(8);
        card.addView(error, errP);
    }

    private void addSendOnly(
            LinearLayout card,
            AppStore.Item item,
            String label
    ) {
        TextView send = actionButton(label, true);
        send.setOnClickListener(v -> sendForAnalysis(item));
        LinearLayout.LayoutParams sendP = matchWrap();
        sendP.topMargin = dp(10);
        card.addView(send, sendP);

        addViewVideoButton(card, item);
    }

    private void addSendAndDone(LinearLayout card, AppStore.Item item) {
        TextView send = actionButton("Mandar para análise", true);
        send.setOnClickListener(v -> sendForAnalysis(item));
        LinearLayout.LayoutParams sendP = matchWrap();
        sendP.topMargin = dp(14);
        card.addView(send, sendP);

        addViewVideoButton(card, item);

        TextView done = actionButton("Marcar como enviado", false);
        done.setOnClickListener(v -> markSent(item));
        LinearLayout.LayoutParams doneP = matchWrap();
        doneP.topMargin = dp(8);
        card.addView(done, doneP);
    }

    private void addViewVideoButton(LinearLayout card, AppStore.Item item) {
        TextView viewVideo = actionButton("Ver vídeo", false);
        viewVideo.setOnClickListener(v -> viewVideo(item));
        LinearLayout.LayoutParams viewP = matchWrap();
        viewP.topMargin = dp(8);
        card.addView(viewVideo, viewP);
    }

    private void viewVideo(AppStore.Item item) {
        if (item.fileUri == null || item.fileUri.isEmpty()) {
            Toast.makeText(this, "Arquivo do vídeo não disponível", Toast.LENGTH_LONG).show();
            return;
        }

        Uri uri = Uri.parse(item.fileUri);
        if (!FolderManager.exists(this, uri)) {
            store.markFileMissing(item.id);
            refresh();
            Toast.makeText(this, "O arquivo do vídeo não foi encontrado", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            Intent view = new Intent(Intent.ACTION_VIEW);
            view.setDataAndType(uri, "video/*");
            view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            view.setClipData(ClipData.newRawUri("Vídeo", uri));
            startActivity(Intent.createChooser(view, "Ver vídeo"));
        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Não foi possível abrir o vídeo neste aparelho",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void retryAnalysis(AppStore.Item item) {
        if (item.fileUri == null ||
                item.fileUri.isEmpty() ||
                !FolderManager.exists(this, Uri.parse(item.fileUri))) {
            store.markFileMissing(item.id);
            refresh();
            return;
        }

        if (!LocalAiModelManager.isModelDownloaded(this)) {
            store.markAiSetupRequired(
                    item.id,
                    item.fileUri,
                    item.fileName,
                    item.title
            );
            startModelDownload();
            refresh();
            return;
        }

        store.markAnalyzing(item.id);
        startMonitorService();
        refresh();
    }

    private void reconcileMissingFilesAsync() {
        if (reconcilingMissingFiles) return;
        reconcilingMissingFiles = true;

        io.execute(() -> {
            boolean changed = false;
            try {
                for (AppStore.Item item : store.getExpectedPhysicalFiles()) {
                    if (item.fileUri == null ||
                            item.fileUri.isEmpty() ||
                            !FolderManager.exists(this, Uri.parse(item.fileUri))) {
                        store.markFileMissing(item.id);
                        changed = true;
                    }
                }
            } finally {
                reconcilingMissingFiles = false;
            }

            if (changed) {
                runOnUiThread(this::refresh);
            }
        });
    }

    private void confirmRemoveMissing(AppStore.Item item) {
        new AlertDialog.Builder(this)
                .setTitle("Remover este registro?")
                .setMessage(
                        "O MP4 já não foi encontrado. " +
                                "Isso remove apenas o registro da lista."
                )
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Remover", (dialog, which) -> {
                    store.deleteRow(item.id);
                    refresh();
                })
                .show();
    }

    private void chooseFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION |
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                        Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
        );
        startActivityForResult(intent, REQUEST_FOLDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_FOLDER && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                FolderManager.saveTreeUri(this, uri, data.getFlags());
                Toast.makeText(this, "Pasta conectada ✓", Toast.LENGTH_SHORT).show();
                ensureNotificationPermission();
                refresh();
            } catch (Exception e) {
                Toast.makeText(this, "Não foi possível salvar o acesso à pasta", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
        }
    }

    private void sendForAnalysis(AppStore.Item item) {
        StringBuilder text = new StringBuilder();
        text.append("Código: ").append(item.code).append("\n");
        if (item.title != null && !item.title.isEmpty()) {
            text.append("Título: ").append(item.title).append("\n");
        }
        if (item.summary != null && !item.summary.isEmpty()) {
            text.append("Pré-análise local: ").append(item.summary).append("\n");
        }
        if (item.fileName != null && !item.fileName.isEmpty()) {
            text.append("Arquivo: ").append(item.fileName).append("\n");
        }
        text.append("Link original: ").append(item.url).append("\n\n");
        text.append(AppSettings.getAnalysisPrompt(this));

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("Análise do vídeo", text.toString()));

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, text.toString());
        startActivity(Intent.createChooser(share, "Mandar para análise"));
    }

    private void retry(AppStore.Item item) {
        if (!FolderManager.hasFolderAccess(this)) {
            Toast.makeText(this, "Conecte a pasta primeiro", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            store.markDownloading(item.id);
            YtdlnisHelper.sendCommandDownload(this, item.url);
            startMonitorService();
            Toast.makeText(this, "Enviado novamente ao YTDLnis", Toast.LENGTH_SHORT).show();
            refresh();
        } catch (Exception e) {
            store.markError(item.id, "Não foi possível abrir o YTDLnis");
            refresh();
        }
    }

    private void markSent(AppStore.Item item) {
        if (item.fileUri == null || item.fileUri.isEmpty()) return;

        Toast.makeText(this, "Movendo para a lixeira…", Toast.LENGTH_SHORT).show();
        io.execute(() -> {
            try {
                Uri moved = FolderManager.moveToTrash(this, Uri.parse(item.fileUri), item.fileName);
                store.markTrashed(item.id, moved.toString());
                runOnUiThread(() -> {
                    Toast.makeText(this, "Enviado • lixeira por 7 dias", Toast.LENGTH_SHORT).show();
                    refresh();
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Não foi possível mover o arquivo", Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    private void restore(AppStore.Item item) {
        if (item.fileUri == null || item.fileUri.isEmpty()) return;

        io.execute(() -> {
            try {
                Uri moved = FolderManager.restoreFromTrash(this, Uri.parse(item.fileUri), item.fileName);
                store.restore(item.id, moved.toString());
                runOnUiThread(() -> {
                    Toast.makeText(this, "Restaurado ✓", Toast.LENGTH_SHORT).show();
                    refresh();
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Não foi possível restaurar", Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    private void deleteNow(AppStore.Item item) {
        io.execute(() -> {
            boolean deleted = deleteTrashFileOrMissing(item);
            if (deleted) {
                store.deleteRow(item.id);
            }

            boolean finalDeleted = deleted;
            runOnUiThread(() -> {
                if (!finalDeleted) {
                    Toast.makeText(
                            this,
                            "Não foi possível apagar o arquivo físico",
                            Toast.LENGTH_LONG
                    ).show();
                }
                refresh();
            });
        });
    }

    private void confirmEmptyTrash() {
        List<AppStore.Item> trash = store.getTrash();
        if (trash.isEmpty()) return;

        new AlertDialog.Builder(this)
                .setTitle("Esvaziar lixeira?")
                .setMessage(
                        "Isso excluirá permanentemente " + trash.size() +
                                " vídeo(s), apagará os registros e liberará os códigos. " +
                                "Essa ação não pode ser desfeita."
                )
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Excluir tudo", (dialog, which) -> emptyTrashPermanently())
                .show();
    }

    private void emptyTrashPermanently() {
        Toast.makeText(this, "Esvaziando lixeira…", Toast.LENGTH_SHORT).show();

        io.execute(() -> {
            int deleted = 0;
            int failed = 0;

            for (AppStore.Item item : store.getTrash()) {
                if (deleteTrashFileOrMissing(item)) {
                    store.deleteRow(item.id);
                    deleted++;
                } else {
                    failed++;
                }
            }

            int deletedCount = deleted;
            int failedCount = failed;
            runOnUiThread(() -> {
                if (failedCount == 0) {
                    Toast.makeText(
                            this,
                            deletedCount + " item(ns) excluído(s) permanentemente",
                            Toast.LENGTH_LONG
                    ).show();
                } else {
                    Toast.makeText(
                            this,
                            deletedCount + " excluído(s) • " +
                                    failedCount + " não puderam ser apagados",
                            Toast.LENGTH_LONG
                    ).show();
                }
                refresh();
            });
        });
    }

    private boolean deleteTrashFileOrMissing(AppStore.Item item) {
        if (item.fileUri == null || item.fileUri.isEmpty()) return true;

        Uri uri = Uri.parse(item.fileUri);
        if (!FolderManager.exists(this, uri)) return true;
        return FolderManager.delete(this, uri);
    }

    private void purgeExpiredTrashAsync() {
        io.execute(() -> {
            long cutoff = System.currentTimeMillis() - TRASH_RETENTION_MS;
            boolean changed = false;

            for (AppStore.Item item : store.getTrash()) {
                if (item.trashAt > 0 && item.trashAt <= cutoff) {
                    if (deleteTrashFileOrMissing(item)) {
                        store.deleteRow(item.id);
                        changed = true;
                    }
                }
            }

            if (changed) runOnUiThread(this::refresh);
        });
    }

    private void showSettings() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(true);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(20), dp(20), dp(20), dp(16));
        panel.setBackground(rounded(SURFACE, BORDER, 26));

        TextView dialogTitle = text("Configurações", 22, true);
        dialogTitle.setTextColor(TEXT);
        panel.addView(dialogTitle);

        TextView dialogSubtitle = text(
                "Ajuste o prompt e mantenha o aplicativo atualizado.",
                13,
                false
        );
        dialogSubtitle.setTextColor(MUTED);
        LinearLayout.LayoutParams subtitleP = matchWrap();
        subtitleP.topMargin = dp(5);
        subtitleP.bottomMargin = dp(16);
        panel.addView(dialogSubtitle, subtitleP);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, 0, 0, dp(8));
        scroll.addView(
                content,
                new ScrollView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        TextView promptTitle = text("Prompt de análise", 16, true);
        promptTitle.setTextColor(TEXT);
        content.addView(promptTitle);

        TextView promptHint = text(
                "Este texto é acrescentado depois do código, título, pré-análise, arquivo e link.",
                13,
                false
        );
        promptHint.setTextColor(MUTED);
        LinearLayout.LayoutParams promptHintP = matchWrap();
        promptHintP.topMargin = dp(5);
        content.addView(promptHint, promptHintP);

        EditText promptInput = new EditText(this);
        promptInput.setText(AppSettings.getAnalysisPrompt(this));
        promptInput.setTextSize(14);
        promptInput.setTextColor(TEXT);
        promptInput.setMinLines(7);
        promptInput.setGravity(Gravity.TOP | Gravity.START);
        promptInput.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                        InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        );
        promptInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        promptInput.setBackground(rounded(Color.WHITE, BORDER, 14));
        LinearLayout.LayoutParams promptP = matchWrap();
        promptP.topMargin = dp(10);
        content.addView(promptInput, promptP);

        TextView savePrompt = actionButton("Salvar prompt", true);
        LinearLayout.LayoutParams savePromptP = matchWrap();
        savePromptP.topMargin = dp(10);
        content.addView(savePrompt, savePromptP);

        TextView resetPrompt = actionButton("Restaurar prompt padrão", false);
        LinearLayout.LayoutParams resetPromptP = matchWrap();
        resetPromptP.topMargin = dp(8);
        resetPromptP.bottomMargin = dp(22);
        content.addView(resetPrompt, resetPromptP);

        TextView updateTitle = text("Atualizações", 16, true);
        updateTitle.setTextColor(TEXT);
        content.addView(updateTitle);

        TextView updateStatus = text(
                "Versão instalada: v" + getVersionName(),
                13,
                false
        );
        updateStatus.setTextColor(MUTED);
        LinearLayout.LayoutParams updateStatusP = matchWrap();
        updateStatusP.topMargin = dp(6);
        content.addView(updateStatus, updateStatusP);

        TextView updateAction = actionButton("Verificar atualização", false);
        LinearLayout.LayoutParams updateActionP = matchWrap();
        updateActionP.topMargin = dp(10);
        updateActionP.bottomMargin = dp(4);
        content.addView(updateAction, updateActionP);

        LinearLayout.LayoutParams scrollP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        );
        panel.addView(scroll, scrollP);

        TextView close = actionButton("Fechar", false);
        close.setTextColor(RED);
        LinearLayout.LayoutParams closeP = matchWrap();
        closeP.topMargin = dp(12);
        panel.addView(close, closeP);

        savePrompt.setOnClickListener(v -> {
            AppSettings.saveAnalysisPrompt(this, promptInput.getText().toString());
            Toast.makeText(this, "Prompt salvo ✓", Toast.LENGTH_SHORT).show();
        });

        resetPrompt.setOnClickListener(v -> {
            AppSettings.resetAnalysisPrompt(this);
            promptInput.setText(AppSettings.DEFAULT_ANALYSIS_PROMPT);
            Toast.makeText(this, "Prompt padrão restaurado", Toast.LENGTH_SHORT).show();
        });

        close.setOnClickListener(v -> dialog.dismiss());

        configureUpdateControls(updateStatus, updateAction);

        dialog.setContentView(panel);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setDimAmount(0.45f);
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }

        dialog.show();

        window = dialog.getWindow();
        if (window != null) {
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            window.setLayout(
                    Math.min(screenWidth - dp(28), dp(560)),
                    Math.min((int) (screenHeight * 0.86f), dp(760))
            );
        }
    }

    private void configureUpdateControls(TextView status, TextView action) {
        AppUpdateManager.DownloadState download =
                AppUpdateManager.getDownloadState(this);
        String target = AppUpdateManager.getSavedTargetVersion(this);

        if (download.isSuccessful() && target != null && !target.isEmpty()) {
            status.setText(
                    "v" + target + " baixada e pronta para instalar"
            );
            status.setTextColor(GREEN);
            action.setText("Instalar atualização");
            action.setClickable(true);
            action.setOnClickListener(v -> {
                if (!AppUpdateManager.installDownloadedUpdate(this) &&
                        (Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                                getPackageManager().canRequestPackageInstalls())) {
                    Toast.makeText(
                            this,
                            "Não foi possível abrir o instalador",
                            Toast.LENGTH_LONG
                    ).show();
                }
            });
            return;
        }

        if (download.isActive()) {
            int percent = download.percent();
            status.setText(
                    percent >= 0
                            ? "Baixando atualização • " + percent + "%"
                            : "Baixando atualização…"
            );
            status.setTextColor(AMBER);
            action.setText("Download em andamento");
            action.setClickable(false);
            return;
        }

        if (download.isFailed()) {
            status.setText("O último download de atualização falhou");
            status.setTextColor(RED);
        } else {
            status.setText("Versão instalada: v" + getVersionName());
            status.setTextColor(MUTED);
        }

        action.setText("Verificar atualização");
        action.setClickable(true);
        action.setOnClickListener(v -> checkForUpdate(status, action));
    }

    private void checkForUpdate(TextView status, TextView action) {
        action.setText("Verificando…");
        action.setClickable(false);
        status.setText("Consultando GitHub Releases…");
        status.setTextColor(MUTED);

        io.execute(() -> {
            try {
                AppUpdateManager.UpdateInfo info =
                        AppUpdateManager.checkLatest(this);

                runOnUiThread(() -> {
                    if (!info.updateAvailable) {
                        status.setText(
                                "Você já está na versão mais recente: v" +
                                        getVersionName()
                        );
                        status.setTextColor(GREEN);
                        action.setText("Verificar novamente");
                        action.setClickable(true);
                        action.setOnClickListener(
                                v -> checkForUpdate(status, action)
                        );
                        return;
                    }

                    status.setText(
                            "Nova versão disponível: v" + info.version
                    );
                    status.setTextColor(GREEN);
                    action.setText("Baixar e instalar v" + info.version);
                    action.setClickable(true);
                    action.setOnClickListener(v -> {
                        try {
                            AppUpdateManager.startDownload(this, info);
                            status.setText(
                                    "Download iniciado. Ao terminar, " +
                                            "toque na notificação para instalar."
                            );
                            status.setTextColor(AMBER);
                            action.setText("Download em andamento");
                            action.setClickable(false);
                            Toast.makeText(
                                    this,
                                    "Atualização sendo baixada",
                                    Toast.LENGTH_LONG
                            ).show();
                        } catch (Exception e) {
                            status.setText(
                                    "Não foi possível iniciar o download"
                            );
                            status.setTextColor(RED);
                            action.setText("Tentar novamente");
                            action.setClickable(true);
                            action.setOnClickListener(
                                    retry -> checkForUpdate(status, action)
                            );
                        }
                    });
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    status.setText(
                            "Não foi possível verificar atualizações"
                    );
                    status.setTextColor(RED);
                    action.setText("Tentar novamente");
                    action.setClickable(true);
                    action.setOnClickListener(
                            v -> checkForUpdate(status, action)
                    );
                });
            }
        });
    }

    private void startMonitorService() {
        Intent service = new Intent(this, DownloadMonitorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(service);
        } else {
            startService(service);
        }
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(rounded(SURFACE, BORDER, 22));
        card.setElevation(dp(2));
        return card;
    }

    private TextView tab(String value, boolean selected) {
        TextView view = text(value, 14, true);
        view.setGravity(Gravity.CENTER);
        view.setTextColor(selected ? RED : MUTED);
        view.setBackground(rounded(selected ? RED_SOFT : Color.TRANSPARENT, selected ? RED_SOFT : BORDER, 18));
        return view;
    }

    private TextView actionButton(String value, boolean primary) {
        TextView view = text(value, 15, true);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(14), dp(13), dp(14), dp(13));
        view.setTextColor(primary ? Color.WHITE : TEXT);
        view.setBackground(rounded(primary ? RED : Color.rgb(247, 244, 243), primary ? RED : BORDER, 18));
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    private TextView chip(String value, int bg, int color) {
        TextView view = text(value, 12, true);
        view.setGravity(Gravity.CENTER);
        view.setTextColor(color);
        view.setPadding(dp(11), dp(7), dp(11), dp(7));
        view.setBackground(rounded(bg, bg, 99));
        return view;
    }

    private TextView label(String value) {
        TextView view = text(value, 11, true);
        view.setTextColor(MUTED);
        view.setLetterSpacing(0.08f);
        return view;
    }

    private TextView text(String value, int size, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setGravity(Gravity.START);
        view.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        return view;
    }

    private GradientDrawable rounded(int fill, int stroke, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private LinearLayout.LayoutParams cardSpacing() {
        LinearLayout.LayoutParams p = matchWrap();
        p.bottomMargin = dp(12);
        return p;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams wrapWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String getVersionName() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            return info.versionName == null ? "0.2.0" : info.versionName;
        } catch (Exception ignored) {
            return "0.2.0";
        }
    }

    @Override
    protected void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }
}
