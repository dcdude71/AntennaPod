package de.danoeh.antennapod.storage.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.danoeh.antennapod.model.feed.AdvancedEpisodeFilter;

public class EpisodeFilterDbAdapter {
    public static final String TABLE_NAME_FEED_EPISODE_FILTERS = "FeedEpisodeFilters";

    private static final String KEY_FEED = "feed";
    private static final String KEY_INCLUDE = "include_terms";
    private static final String KEY_MATCH_ALL = "match_all";
    private static final String KEY_EXCLUDE = "exclude_terms";
    private static final String KEY_SEARCH_TITLE = "search_title";
    private static final String KEY_SEARCH_DESCRIPTION = "search_description";
    private static final String KEY_WHOLE_WORDS = "whole_words";
    private static final String KEY_MIN_DURATION = "min_duration";
    private static final String KEY_MAX_DURATION = "max_duration";
    private static final String TERM_SEPARATOR = "\n";
    private static final int MAX_IDS_PER_QUERY = 500;

    private static final String CREATE_TABLE_FEED_EPISODE_FILTERS = "CREATE TABLE IF NOT EXISTS "
            + TABLE_NAME_FEED_EPISODE_FILTERS + " ("
            + KEY_FEED + " INTEGER PRIMARY KEY, "
            + KEY_INCLUDE + " TEXT NOT NULL DEFAULT '', "
            + KEY_MATCH_ALL + " INTEGER NOT NULL DEFAULT 0, "
            + KEY_EXCLUDE + " TEXT NOT NULL DEFAULT '', "
            + KEY_SEARCH_TITLE + " INTEGER NOT NULL DEFAULT 1, "
            + KEY_SEARCH_DESCRIPTION + " INTEGER NOT NULL DEFAULT 0, "
            + KEY_WHOLE_WORDS + " INTEGER NOT NULL DEFAULT 0, "
            + KEY_MIN_DURATION + " INTEGER NOT NULL DEFAULT -1, "
            + KEY_MAX_DURATION + " INTEGER NOT NULL DEFAULT -1)";

    private final SQLiteDatabase db;

    EpisodeFilterDbAdapter(SQLiteDatabase db) {
        this.db = db;
    }

    static void createTables(SQLiteDatabase db) {
        if (db.isReadOnly()) {
            return;
        }
        db.execSQL(CREATE_TABLE_FEED_EPISODE_FILTERS);
    }

    @Nullable
    AdvancedEpisodeFilter getFilter(long feedId) {
        try (Cursor cursor = db.query(TABLE_NAME_FEED_EPISODE_FILTERS, null, KEY_FEED + "=?",
                new String[]{String.valueOf(feedId)}, null, null, null)) {
            if (!cursor.moveToFirst()) {
                return null;
            }
            return new AdvancedEpisodeFilter(
                    splitTerms(cursor.getString(cursor.getColumnIndexOrThrow(KEY_INCLUDE))),
                    cursor.getInt(cursor.getColumnIndexOrThrow(KEY_MATCH_ALL)) != 0,
                    splitTerms(cursor.getString(cursor.getColumnIndexOrThrow(KEY_EXCLUDE))),
                    cursor.getInt(cursor.getColumnIndexOrThrow(KEY_SEARCH_TITLE)) != 0,
                    cursor.getInt(cursor.getColumnIndexOrThrow(KEY_SEARCH_DESCRIPTION)) != 0,
                    cursor.getInt(cursor.getColumnIndexOrThrow(KEY_WHOLE_WORDS)) != 0,
                    cursor.getInt(cursor.getColumnIndexOrThrow(KEY_MIN_DURATION)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(KEY_MAX_DURATION)));
        }
    }

    void setFilter(long feedId, @NonNull AdvancedEpisodeFilter filter) {
        ContentValues values = new ContentValues();
        values.put(KEY_FEED, feedId);
        values.put(KEY_INCLUDE, TextUtils.join(TERM_SEPARATOR, filter.getIncludeTerms()));
        values.put(KEY_MATCH_ALL, filter.isMatchAll() ? 1 : 0);
        values.put(KEY_EXCLUDE, TextUtils.join(TERM_SEPARATOR, filter.getExcludeTerms()));
        values.put(KEY_SEARCH_TITLE, filter.isSearchTitle() ? 1 : 0);
        values.put(KEY_SEARCH_DESCRIPTION, filter.isSearchDescription() ? 1 : 0);
        values.put(KEY_WHOLE_WORDS, filter.isWholeWords() ? 1 : 0);
        values.put(KEY_MIN_DURATION, filter.getMinDurationSeconds());
        values.put(KEY_MAX_DURATION, filter.getMaxDurationSeconds());
        db.insertWithOnConflict(TABLE_NAME_FEED_EPISODE_FILTERS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    void deleteFeed(long feedId) {
        db.delete(TABLE_NAME_FEED_EPISODE_FILTERS, KEY_FEED + "=?", new String[]{String.valueOf(feedId)});
    }

    @NonNull
    Map<Long, String> getDescriptions(@NonNull List<Long> itemIds) {
        Map<Long, String> descriptions = new HashMap<>();
        for (int start = 0; start < itemIds.size(); start += MAX_IDS_PER_QUERY) {
            List<Long> chunk = itemIds.subList(start, Math.min(itemIds.size(), start + MAX_IDS_PER_QUERY));
            String query = "SELECT " + PodDBAdapter.KEY_ID + ", " + PodDBAdapter.KEY_DESCRIPTION
                    + " FROM " + PodDBAdapter.TABLE_NAME_FEED_ITEMS
                    + " WHERE " + PodDBAdapter.KEY_ID + " IN (" + TextUtils.join(",", chunk) + ")";
            try (Cursor cursor = db.rawQuery(query, null)) {
                while (cursor.moveToNext()) {
                    descriptions.put(cursor.getLong(0), cursor.getString(1));
                }
            }
        }
        return descriptions;
    }

    @NonNull
    private static List<String> splitTerms(@Nullable String value) {
        if (TextUtils.isEmpty(value)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(value.split(TERM_SEPARATOR)));
    }
}
