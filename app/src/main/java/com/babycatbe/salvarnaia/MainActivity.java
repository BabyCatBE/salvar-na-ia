package com.babycatbe.salvarnaia;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends Activity {

    private static final int COLOR_BG = Color.rgb(249, 246, 244);
    private static final int COLOR_SURFACE = Color.WHITE;
    private static final int COLOR_TEXT = Color.rgb(38, 32, 31);
    private static final int COLOR_MUTED = Color.rgb(116, 105, 102);
    private static final int COLOR_RED = Color.rgb(183, 28, 28);
    private static final int COLOR_RED_SOFT = Color.rgb(255, 238, 236);
    private static final int COLOR_BORDER = Color.rgb(235, 226, 223);
    private static final int COLOR_GREEN = Color.rgb(38, 126, 71);

    private LinearLayout content;
    private TextView statusValue;
    private TextView statusHint;
    private TextView countValue;
    private TextView urlValue;
    private TextView receivedAtValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureSystemBars();
        buildUi();
        animateEntrance();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void configureSystemBars() {
        Window window = getWindow();
        window.setStatusBarColor(COLOR_BG);
        window.setNavigationBarColor(COLOR_BG);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(COLOR_BG);
        scroll.setClipToPadding(false);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(28), dp(20), dp(32));
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView eyebrow = text("SALVAR NA IA", 12, true);
        eyebrow.setTextColor(COLOR_RED);
        eyebrow.setLetterSpacing(0.12f);
        content.addView(eyebrow);

        TextView title = text("Seus links, prontos para o próximo passo.", 29, true);
        title.setTextColor(COLOR_TEXT);
        title.setLineSpacing(0f, 1.05f);
        LinearLayout.LayoutParams titleParams = matchWrap();
        titleParams.topMargin = dp(8);
        content.addView(title, titleParams);

        TextView subtitle = text(
                "Compartilhe pelo Instagram ou TikTok e continue usando o celular normalmente.",
                15,
                false
        );
        subtitle.setTextColor(COLOR_MUTED);
        subtitle.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams subtitleParams = matchWrap();
        subtitleParams.topMargin = dp(10);
        subtitleParams.bottomMargin = dp(22);
        content.addView(subtitle, subtitleParams);

        LinearLayout phaseRow = new LinearLayout(this);
        phaseRow.setOrientation(LinearLayout.HORIZONTAL);
        phaseRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView versionChip = chip("v" + getVersionName(), COLOR_RED_SOFT, COLOR_RED);
        phaseRow.addView(versionChip);

        TextView phaseChip = chip("Fase 1 • Share Target", Color.rgb(241, 239, 238), COLOR_MUTED);
        LinearLayout.LayoutParams phaseParams = wrapWrap();
        phaseParams.leftMargin = dp(8);
        phaseRow.addView(phaseChip, phaseParams);

        LinearLayout.LayoutParams phaseRowParams = matchWrap();
        phaseRowParams.bottomMargin = dp(18);
        content.addView(phaseRow, phaseRowParams);

        LinearLayout statusCard = card();
        TextView statusLabel = label("STATUS");
        statusCard.addView(statusLabel);

        statusValue = text("", 19, true);
        statusValue.setTextColor(COLOR_TEXT);
        LinearLayout.LayoutParams statusValueParams = matchWrap();
        statusValueParams.topMargin = dp(8);
        statusCard.addView(statusValue, statusValueParams);

        statusHint = text("", 14, false);
        statusHint.setTextColor(COLOR_MUTED);
        statusHint.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams statusHintParams = matchWrap();
        statusHintParams.topMargin = dp(6);
        statusCard.addView(statusHint, statusHintParams);

        content.addView(statusCard, cardParams(dp(0), dp(14)));

        LinearLayout statsRow = new LinearLayout(this);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        statsRow.setWeightSum(2f);

        LinearLayout countCard = compactCard();
        countCard.addView(label("RECEBIDOS"));
        countValue = text("", 28, true);
        countValue.setTextColor(COLOR_TEXT);
        LinearLayout.LayoutParams countValueParams = matchWrap();
        countValueParams.topMargin = dp(5);
        countCard.addView(countValue, countValueParams);

        LinearLayout.LayoutParams halfLeft = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        halfLeft.rightMargin = dp(7);
        statsRow.addView(countCard, halfLeft);

        LinearLayout timeCard = compactCard();
        timeCard.addView(label("ÚLTIMO"));
        receivedAtValue = text("", 15, true);
        receivedAtValue.setTextColor(COLOR_TEXT);
        receivedAtValue.setMaxLines(2);
        LinearLayout.LayoutParams timeValueParams = matchWrap();
        timeValueParams.topMargin = dp(8);
        timeCard.addView(receivedAtValue, timeValueParams);

        LinearLayout.LayoutParams halfRight = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        halfRight.leftMargin = dp(7);
        statsRow.addView(timeCard, halfRight);

        LinearLayout.LayoutParams statsParams = matchWrap();
        statsParams.bottomMargin = dp(14);
        content.addView(statsRow, statsParams);

        LinearLayout linkCard = card();
        linkCard.addView(label("ÚLTIMO LINK RECEBIDO"));

        urlValue = text("", 16, false);
        urlValue.setTextColor(COLOR_TEXT);
        urlValue.setTextIsSelectable(true);
        urlValue.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams urlParams = matchWrap();
        urlParams.topMargin = dp(10);
        linkCard.addView(urlValue, urlParams);

        TextView linkHint = text("Toque e segure para selecionar ou copiar.", 12, false);
        linkHint.setTextColor(COLOR_MUTED);
        LinearLayout.LayoutParams linkHintParams = matchWrap();
        linkHintParams.topMargin = dp(10);
        linkCard.addView(linkHint, linkHintParams);

        content.addView(linkCard, cardParams(dp(0), dp(18)));

        TextView nextTitle = text("Próximo passo", 16, true);
        nextTitle.setTextColor(COLOR_TEXT);
        content.addView(nextTitle);

        TextView nextText = text(
                "A Fase 1 já está validada. A próxima etapa será conectar o recebimento ao YTDLnis, mantendo o fluxo simples e local.",
                14,
                false
        );
        nextText.setTextColor(COLOR_MUTED);
        nextText.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams nextParams = matchWrap();
        nextParams.topMargin = dp(6);
        content.addView(nextText, nextParams);

        setContentView(scroll);
    }

    private void refreshStatus() {
        SharedPreferences prefs = getSharedPreferences(
                ShareReceiverActivity.PREFS_NAME,
                MODE_PRIVATE
        );

        int count = prefs.getInt(ShareReceiverActivity.KEY_RECEIVED_COUNT, 0);
        String url = prefs.getString(ShareReceiverActivity.KEY_LAST_URL, "");
        long receivedAt = prefs.getLong(ShareReceiverActivity.KEY_LAST_RECEIVED_AT, 0L);

        if (count > 0) {
            statusValue.setText("Tudo funcionando ✓");
            statusValue.setTextColor(COLOR_GREEN);
            statusHint.setText("O app está recebendo os compartilhamentos corretamente.");
        } else {
            statusValue.setText("Aguardando primeiro link");
            statusValue.setTextColor(COLOR_TEXT);
            statusHint.setText("Compartilhe um vídeo pelo Instagram ou TikTok para testar.");
        }

        countValue.setText(String.valueOf(count));
        urlValue.setText(url == null || url.isEmpty() ? "Nenhum link recebido ainda." : url);

        if (receivedAt > 0L) {
            String formatted = DateFormat.getDateTimeInstance(
                    DateFormat.SHORT,
                    DateFormat.SHORT
            ).format(new Date(receivedAt));
            receivedAtValue.setText(formatted);
        } else {
            receivedAtValue.setText("—");
        }
    }

    private void animateEntrance() {
        for (int i = 0; i < content.getChildCount(); i++) {
            View child = content.getChildAt(i);
            child.setAlpha(0f);
            child.setTranslationY(dp(10));

            ObjectAnimator fade = ObjectAnimator.ofFloat(child, View.ALPHA, 0f, 1f);
            ObjectAnimator slide = ObjectAnimator.ofFloat(child, View.TRANSLATION_Y, dp(10), 0f);

            AnimatorSet set = new AnimatorSet();
            set.playTogether(fade, slide);
            set.setDuration(260);
            set.setStartDelay(i * 45L);
            set.setInterpolator(new DecelerateInterpolator());
            set.start();
        }
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(roundedBackground(COLOR_SURFACE, COLOR_BORDER, 22));
        card.setElevation(dp(2));
        return card;
    }

    private LinearLayout compactCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(roundedBackground(COLOR_SURFACE, COLOR_BORDER, 20));
        card.setElevation(dp(1));
        return card;
    }

    private TextView chip(String contentText, int backgroundColor, int textColor) {
        TextView chip = text(contentText, 12, true);
        chip.setTextColor(textColor);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(11), dp(7), dp(11), dp(7));
        chip.setBackground(roundedBackground(backgroundColor, backgroundColor, 99));
        return chip;
    }

    private GradientDrawable roundedBackground(int fillColor, int strokeColor, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private TextView label(String contentText) {
        TextView view = text(contentText, 11, true);
        view.setTextColor(COLOR_MUTED);
        view.setLetterSpacing(0.08f);
        return view;
    }

    private TextView text(String contentText, int sizeSp, boolean bold) {
        TextView view = new TextView(this);
        view.setText(contentText);
        view.setTextSize(sizeSp);
        view.setGravity(Gravity.START);
        view.setFontFeatureSettings("kern");
        if (bold) {
            view.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        } else {
            view.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        }
        return view;
    }

    private LinearLayout.LayoutParams cardParams(int top, int bottom) {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = top;
        params.bottomMargin = bottom;
        return params;
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
            return info.versionName == null ? "0.1.1" : info.versionName;
        } catch (Exception ignored) {
            return "0.1.1";
        }
    }
}
