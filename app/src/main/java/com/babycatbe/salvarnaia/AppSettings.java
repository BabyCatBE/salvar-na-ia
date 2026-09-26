package com.babycatbe.salvarnaia;

import android.content.Context;
import android.content.SharedPreferences;

public final class AppSettings {

    private static final String PREFS = "salvar_na_ia_settings";
    private static final String KEY_ANALYSIS_PROMPT = "analysis_prompt";

    public static final String DEFAULT_ANALYSIS_PROMPT =
            "Analise o vídeo completo usando o MP4 anexado. " +
            "Identifique os principais pontos e as informações práticas relevantes. " +
            "Use a pré-análise local apenas como contexto: confirme, corrija e complemente. " +
            "Ao final, salve a análise organizada no Notion diretamente dentro da página raiz " +
            "\"Análises de Vídeos\" " +
            "(https://app.notion.com/p/3e76aebea38681398443cc489e718f40?pvs=204), " +
            "criando uma NOVA subpágina diretamente filha dessa página no formato " +
            "\"CÓDIGO — TÍTULO\". Não crie a análise dentro de uma análise anterior e " +
            "não crie níveis encadeados de páginas. Se não houver acesso ao Notion, " +
            "retorne a análise normalmente no chat.";

    private AppSettings() {
    }

    public static String getAnalysisPrompt(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String value = prefs.getString(KEY_ANALYSIS_PROMPT, DEFAULT_ANALYSIS_PROMPT);
        if (value == null || value.trim().isEmpty()) return DEFAULT_ANALYSIS_PROMPT;
        return value;
    }

    public static void saveAnalysisPrompt(Context context, String prompt) {
        String value = prompt == null ? "" : prompt.trim();
        if (value.isEmpty()) value = DEFAULT_ANALYSIS_PROMPT;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ANALYSIS_PROMPT, value)
                .apply();
    }

    public static void resetAnalysisPrompt(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_ANALYSIS_PROMPT)
                .apply();
    }
}
