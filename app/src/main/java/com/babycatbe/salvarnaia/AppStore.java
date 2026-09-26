package com.babycatbe.salvarnaia;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppStore extends SQLiteOpenHelper {

    public static final String STATUS_DOWNLOADING = "DOWNLOADING";
    public static final String STATUS_READY = "READY";
    public static final String STATUS_ERROR = "ERROR";
    public static final String STATUS_TRASHED = "TRASHED";

    private static final String DB_NAME = "salvar_na_ia.db";
    private static final int DB_VERSION = 1;

    public AppStore(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE items (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "code TEXT NOT NULL DEFAULT ''," +
                        "url TEXT NOT NULL," +
                        "status TEXT NOT NULL," +
                        "title TEXT NOT NULL DEFAULT ''," +
                        "summary TEXT NOT NULL DEFAULT ''," +
                        "file_uri TEXT NOT NULL DEFAULT ''," +
                        "file_name TEXT NOT NULL DEFAULT ''," +
                        "error TEXT NOT NULL DEFAULT ''," +
                        "created_at INTEGER NOT NULL," +
                        "updated_at INTEGER NOT NULL," +
                        "trash_at INTEGER NOT NULL DEFAULT 0" +
                        ")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public synchronized Item addDownloading(String url) {
        long now = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put("url", url);
        values.put("status", STATUS_DOWNLOADING);
        values.put("created_at", now);
        values.put("updated_at", now);

        SQLiteDatabase db = getWritableDatabase();
        long id = db.insertOrThrow("items", null, values);

        String code = codeFor(id);
        ContentValues codeValues = new ContentValues();
        codeValues.put("code", code);
        db.update("items", codeValues, "id=?", new String[]{String.valueOf(id)});
        return get(id);
    }

    public synchronized Item get(long id) {
        try (Cursor c = getReadableDatabase().query(
                "items",
                null,
                "id=?",
                new String[]{String.valueOf(id)},
                null,
                null,
                null
        )) {
            if (c.moveToFirst()) return fromCursor(c);
        }
        return null;
    }

    public synchronized List<Item> getPending() {
        return query(
                "status IN (?,?,?)",
                new String[]{STATUS_DOWNLOADING, STATUS_READY, STATUS_ERROR},
                "created_at DESC"
        );
    }

    public synchronized List<Item> getTrash() {
        return query(
                "status=?",
                new String[]{STATUS_TRASHED},
                "trash_at DESC"
        );
    }

    public synchronized List<Item> getDownloading() {
        return query(
                "status=?",
                new String[]{STATUS_DOWNLOADING},
                "updated_at ASC"
        );
    }

    public synchronized void markReady(long id, String fileUri, String fileName, String title) {
        ContentValues values = new ContentValues();
        values.put("status", STATUS_READY);
        values.put("file_uri", fileUri == null ? "" : fileUri);
        values.put("file_name", fileName == null ? "" : fileName);
        values.put("title", title == null ? "" : title);
        values.put("error", "");
        values.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update("items", values, "id=?", new String[]{String.valueOf(id)});
    }

    public synchronized void markError(long id, String message) {
        ContentValues values = new ContentValues();
        values.put("status", STATUS_ERROR);
        values.put("error", message == null ? "" : message);
        values.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update("items", values, "id=?", new String[]{String.valueOf(id)});
    }

    public synchronized void markDownloading(long id) {
        ContentValues values = new ContentValues();
        values.put("status", STATUS_DOWNLOADING);
        values.put("error", "");
        values.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update("items", values, "id=?", new String[]{String.valueOf(id)});
    }

    public synchronized void markTrashed(long id, String fileUri) {
        long now = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put("status", STATUS_TRASHED);
        values.put("file_uri", fileUri == null ? "" : fileUri);
        values.put("trash_at", now);
        values.put("updated_at", now);
        getWritableDatabase().update("items", values, "id=?", new String[]{String.valueOf(id)});
    }

    public synchronized void restore(long id, String fileUri) {
        ContentValues values = new ContentValues();
        values.put("status", STATUS_READY);
        values.put("file_uri", fileUri == null ? "" : fileUri);
        values.put("trash_at", 0);
        values.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update("items", values, "id=?", new String[]{String.valueOf(id)});
    }

    public synchronized void deleteRow(long id) {
        getWritableDatabase().delete("items", "id=?", new String[]{String.valueOf(id)});
    }

    public synchronized Set<String> getUsedFileUris() {
        Set<String> uris = new HashSet<>();
        try (Cursor c = getReadableDatabase().query(
                "items",
                new String[]{"file_uri"},
                "file_uri<>''",
                null,
                null,
                null,
                null
        )) {
            while (c.moveToNext()) {
                uris.add(c.getString(0));
            }
        }
        return uris;
    }

    private synchronized List<Item> query(String selection, String[] args, String orderBy) {
        List<Item> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query(
                "items",
                null,
                selection,
                args,
                null,
                null,
                orderBy
        )) {
            while (c.moveToNext()) result.add(fromCursor(c));
        }
        return result;
    }

    private Item fromCursor(Cursor c) {
        Item item = new Item();
        item.id = c.getLong(c.getColumnIndexOrThrow("id"));
        item.code = c.getString(c.getColumnIndexOrThrow("code"));
        item.url = c.getString(c.getColumnIndexOrThrow("url"));
        item.status = c.getString(c.getColumnIndexOrThrow("status"));
        item.title = c.getString(c.getColumnIndexOrThrow("title"));
        item.summary = c.getString(c.getColumnIndexOrThrow("summary"));
        item.fileUri = c.getString(c.getColumnIndexOrThrow("file_uri"));
        item.fileName = c.getString(c.getColumnIndexOrThrow("file_name"));
        item.error = c.getString(c.getColumnIndexOrThrow("error"));
        item.createdAt = c.getLong(c.getColumnIndexOrThrow("created_at"));
        item.updatedAt = c.getLong(c.getColumnIndexOrThrow("updated_at"));
        item.trashAt = c.getLong(c.getColumnIndexOrThrow("trash_at"));
        return item;
    }

    public static String codeFor(long id) {
        if (id <= 9) return String.valueOf(id);

        long zeroBased = id - 10;
        long group = zeroBased / 9;
        int position = (int) (zeroBased % 9) + 1;
        return lettersFor(group) + position;
    }

    private static String lettersFor(long value) {
        StringBuilder out = new StringBuilder();
        long n = value;
        do {
            out.insert(0, (char) ('A' + (n % 26)));
            n = (n / 26) - 1;
        } while (n >= 0);
        return out.toString();
    }

    public static class Item {
        public long id;
        public String code;
        public String url;
        public String status;
        public String title;
        public String summary;
        public String fileUri;
        public String fileName;
        public String error;
        public long createdAt;
        public long updatedAt;
        public long trashAt;
    }
}
