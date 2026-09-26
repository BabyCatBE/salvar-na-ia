package com.babycatbe.salvarnaia;

import android.content.Context;
import android.content.Intent;

public class YtdlnisHelper {

    private static final String PACKAGE = "com.deniscerri.ytdl";
    private static final String SHARE_ACTIVITY = "com.deniscerri.ytdl.receiver.ShareActivity";

    public static void sendCommandDownload(Context context, String url) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, url);
        intent.putExtra("TYPE", "command");
        intent.putExtra("BACKGROUND", true);
        intent.setClassName(PACKAGE, SHARE_ACTIVITY);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        context.startActivity(intent);
    }
}
