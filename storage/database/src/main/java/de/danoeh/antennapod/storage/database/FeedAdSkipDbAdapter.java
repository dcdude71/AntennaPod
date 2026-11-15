package de.danoeh.antennapod.storage.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class FeedAdSkipDbAdapter {
    public static final String TABLE_NAME_FEED_AD_SKIP = "FeedAdSkip";

    private static final String KEY_FEED = "feed";
    private static final String KEY_SECONDS = "seconds";

    private static final String CREATE_TABLE_FEED_AD_SKIP = "CREATE TABLE IF NOT EXISTS "
            + TABLE_NAME_FEED_AD_SKIP + " ("
            + KEY_FEED + " INTEGER PRIMARY KEY, "
            + KEY_SECONDS + " INTEGER NOT NULL DEFAULT 0)";

    private final SQLiteDatabase db;

    FeedAdSkipDbAdapter(SQLiteDatabase db) {
        this.db = db;
    }

    static void createTables(SQLiteDatabase db) {
        if (db.isReadOnly()) {
            return;
        }
        db.execSQL(CREATE_TABLE_FEED_AD_SKIP);
    }

    int getSeconds(long feedId) {
        try (Cursor cursor = db.query(TABLE_NAME_FEED_AD_SKIP, new String[]{KEY_SECONDS},
                KEY_FEED + "=?", new String[]{String.valueOf(feedId)}, null, null, null)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    void setSeconds(long feedId, int seconds) {
        if (seconds <= 0) {
            deleteFeed(feedId);
            return;
        }
        ContentValues values = new ContentValues();
        values.put(KEY_FEED, feedId);
        values.put(KEY_SECONDS, seconds);
        db.insertWithOnConflict(TABLE_NAME_FEED_AD_SKIP, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    void deleteFeed(long feedId) {
        db.delete(TABLE_NAME_FEED_AD_SKIP, KEY_FEED + "=?", new String[]{String.valueOf(feedId)});
    }
}
