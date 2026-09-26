package com.babycatbe.salvarnaia;

import android.content.Context;
import android.net.Uri;

import com.google.ai.edge.litertlm.Backend;
import com.google.ai.edge.litertlm.Content;
import com.google.ai.edge.litertlm.Contents;
import com.google.ai.edge.litertlm.Conversation;
import com.google.ai.edge.litertlm.ConversationConfig;
import com.google.ai.edge.litertlm.Engine;
import com.google.ai.edge.litertlm.EngineConfig;
import com.google.ai.edge.litertlm.Message;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LocalAiEngine implements AutoCloseable {

    private final Context context;
    private Engine engine;
    private String backendLabel = "";

    public LocalAiEngine(Context context) throws Exception {
        this.context = context.getApplicationContext();

        if (!LocalAiModelManager.ensureVerified(this.context)) {
            throw new IllegalStateException("Modelo da IA local ausente ou inválido");
        }

        initializeWithFallback();
    }

    private void initializeWithFallback() throws Exception {
        File model = LocalAiModelManager.getModelFile(context);
        Backend.CPU cpu = new Backend.CPU(4, null);

        try {
            EngineConfig gpuConfig = new EngineConfig(
                    model.getAbsolutePath(),
                    new Backend.GPU(),
                    new Backend.GPU(),
                    cpu,
                    8192,
                    5,
                    context.getCacheDir().getAbsolutePath(),
                    null
            );
            engine = new Engine(gpuConfig);
            engine.initialize();
            backendLabel = "GPU";
            return;
        } catch (Throwable gpuError) {
            closeSilently();
        }

        EngineConfig cpuConfig = new EngineConfig(
                model.getAbsolutePath(),
                cpu,
                cpu,
                cpu,
                8192,
                5,
                context.getCacheDir().getAbsolutePath(),
                null
        );
        engine = new Engine(cpuConfig);
        engine.initialize();
        backendLabel = "CPU";
    }

    public AnalysisResult analyze(
            Uri videoUri,
            String fallbackTitle
    ) throws Exception {
        try (VideoAiMedia.PreparedMedia media =
                     VideoAiMedia.prepare(context, videoUri)) {

            List<Content> input = new ArrayList<>();

            for (File image : media.images) {
                input.add(new Content.ImageFile(image.getAbsolutePath()));
            }

            if (media.audio != null) {
                input.add(new Content.AudioFile(media.audio.getAbsolutePath()));
            }

            String prompt =
                    "Analise este vídeo como uma pré-análise local para organização pessoal. " +
                    "Use conjuntamente o conteúdo visual dos frames, textos que aparecem na tela " +
                    "e o áudio/fala disponível. Não invente informações que não estejam no vídeo. " +
                    "Responda em português do Brasil, exatamente neste formato:\n" +
                    "TITULO: um título curto, claro e específico, com no máximo 10 palavras\n" +
                    "RESUMO: de 2 a 4 frases explicando o assunto, a mensagem principal e o que " +
                    "é demonstrado ou ensinado no vídeo.\n" +
                    "Não acrescente outras seções.";

            input.add(new Content.Text(prompt));
            Contents contents = Contents.Companion.of(input);

            Conversation conversation = null;
            try {
                conversation = engine.createConversation(new ConversationConfig());
                Message response = conversation.sendMessage(contents);
                return parseResponse(
                        response.toString(),
                        fallbackTitle,
                        backendLabel,
                        media.durationMs
                );
            } finally {
                if (conversation != null) {
                    try {
                        conversation.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    private static AnalysisResult parseResponse(
            String raw,
            String fallbackTitle,
            String backend,
            long durationMs
    ) {
        String clean = raw == null ? "" : raw.trim();

        Pattern titlePattern = Pattern.compile(
                "(?im)^\\s*\\*{0,2}T[IÍ]TULO\\*{0,2}\\s*:\\s*(.+?)\\s*$"
        );
        Pattern summaryPattern = Pattern.compile(
                "(?is)\\*{0,2}RESUMO\\*{0,2}\\s*:\\s*(.+)$"
        );

        Matcher titleMatcher = titlePattern.matcher(clean);
        Matcher summaryMatcher = summaryPattern.matcher(clean);

        String title = titleMatcher.find()
                ? titleMatcher.group(1).trim()
                : "";
        String summary = summaryMatcher.find()
                ? summaryMatcher.group(1).trim()
                : "";

        title = stripDecorations(title);
        summary = stripDecorations(summary);

        if (title.isEmpty()) {
            title = fallbackTitle == null || fallbackTitle.isBlank()
                    ? "Vídeo analisado"
                    : fallbackTitle;
        }

        if (summary.isEmpty()) {
            summary = clean;
        }

        if (summary.isEmpty()) {
            throw new IllegalStateException("A IA local não retornou um resumo");
        }

        if (title.length() > 80) title = title.substring(0, 80).trim();
        if (summary.length() > 1600) summary = summary.substring(0, 1600).trim();

        return new AnalysisResult(title, summary, backend, durationMs);
    }

    private static String stripDecorations(String value) {
        if (value == null) return "";
        return value
                .replaceAll("^[\\-*#\\s]+", "")
                .replaceAll("[\\*#\\s]+$", "")
                .trim();
    }

    @Override
    public void close() {
        closeSilently();
    }

    private void closeSilently() {
        if (engine != null) {
            try {
                engine.close();
            } catch (Throwable ignored) {
            }
            engine = null;
        }
    }

    public static final class AnalysisResult {
        public final String title;
        public final String summary;
        public final String backend;
        public final long durationMs;

        AnalysisResult(
                String title,
                String summary,
                String backend,
                long durationMs
        ) {
            this.title = title;
            this.summary = summary;
            this.backend = backend;
            this.durationMs = durationMs;
        }

        @Override
        public String toString() {
            return String.format(
                    Locale.US,
                    "%s (%s, %d ms)",
                    title,
                    backend,
                    durationMs
            );
        }
    }
}
