package com.babycatbe.salvarnaia;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends Activity {

    private TextView statusValue;
    private TextView countValue;
    private TextView urlValue;
    private TextView receivedAtValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void buildUi() {
        int padding = dp(24);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(padding, dp(40), padding, padding);
        root.setBackgroundColor(Color.rgb(255, 248, 247));

        TextView title = text("Salvar na IA", 30, true);
        title.setTextColor(Color.rgb(156, 24, 24));
        root.addView(title);

        TextView version = text("v" + getVersionName() + "  •  Fase 1 — Share Target", 14, false);
        version.setTextColor(Color.DKGRAY);
        LinearLayout.LayoutParams versionParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        versionParams.topMargin = dp(4);
        versionParams.bottomMargin = dp(28);
        root.addView(version, versionParams);

        root.addView(label("Status"));
        statusValue = value("");
        root.addView(statusValue, valueParams());

        root.addView(label("Compartilhamentos recebidos"));
        countValue = value("");
        root.addView(countValue, valueParams());

        root.addView(label("Último link recebido"));
        urlValue = value("");
        urlValue.setTextIsSelectable(true);
        root.addView(urlValue, valueParams());

        root.addView(label("Recebido em"));
        receivedAtValue = value("");
        root.addView(receivedAtValue, valueParams());

        TextView note = text(
                "Nesta versão o app apenas recebe e registra o link. YTDLnis e ChatGPT serão adicionados somente depois deste teste passar.",
                14,
                false
        );
        note.setTextColor(Color.GRAY);
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        noteParams.topMargin = dp(16);
        root.addView(note, noteParams);

        setContentView(root);
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
            statusValue.setText("Share Target recebeu conteúdo corretamente ✓");
            statusValue.setTextColor(Color.rgb(34, 110, 58));
        } else {
            statusValue.setText("Aguardando o primeiro compartilhamento");
            statusValue.setTextColor(Color.DKGRAY);
        }

        countValue.setText(String.valueOf(count));
        urlValue.setText(url == null || url.isEmpty() ? "Nenhum ainda" : url);

        if (receivedAt > 0L) {
            String formatted = DateFormat.getDateTimeInstance(
                    DateFormat.SHORT,
                    DateFormat.MEDIUM
            ).format(new Date(receivedAt));
            receivedAtValue.setText(formatted);
        } else {
            receivedAtValue.setText("—");
        }
    }

    private TextView label(String content) {
        TextView view = text(content, 13, true);
        view.setTextColor(Color.rgb(115, 70, 70));
        return view;
    }

    private TextView value(String content) {
        TextView view = text(content, 17, false);
        view.setTextColor(Color.rgb(35, 35, 35));
        view.setPadding(dp(16), dp(14), dp(16), dp(14));
        view.setBackgroundColor(Color.WHITE);
        return view;
    }

    private LinearLayout.LayoutParams valueParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(6);
        params.bottomMargin = dp(18);
        return params;
    }

    private TextView text(String content, int sizeSp, boolean bold) {
        TextView view = new TextView(this);
        view.setText(content);
        view.setTextSize(sizeSp);
        view.setGravity(Gravity.START);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String getVersionName() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            return info.versionName == null ? "0.1" : info.versionName;
        } catch (Exception ignored) {
            return "0.1";
        }
    }
}
