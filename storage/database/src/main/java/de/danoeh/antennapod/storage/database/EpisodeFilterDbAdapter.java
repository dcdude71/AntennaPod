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
import de.danoeh.antennapod.model.feed.EpisodeCondition;

public class EpisodeFilterDbAdapter {
    public static final String TABLE_NAME_FEED_EPISODE_FILTERS = "FeedEpisodeFilters";
    public static final String TABLE_NAME_EPISODE_FILTER_CONDITIONS = "FeedEpisodeFilterConditions";

    private static final String KEY_FEED = "feed";
    private static final String KEY_INCLUDE = "include_terms";
    private static final String KEY_MATCH_ALL = "match_all";
    private static final String KEY_EXCLUDE = "exclude_terms";
    private static final String KEY_SEARCH_TITLE = "search_title";
    private static final String KEY_SEARCH_DESCRIPTION = "search_description";
    private static final String KEY_WHOLE_WORDS = "whole_words";
    private static final String KEY_MIN_DURATION = "min_duration";
    private static final String KEY_MAX_DURATION = "max_duration";
    private static final String KEY_SKIP_MATCH_ALL = "skip_match_all";
    private static final String KEY_FORMAT = "format";
    private static final String KEY_SECTION = "section";
    private static final String KEY_POSITION = "position";
    private static final String KEY_FIELD = "field";
    private static final String KEY_OPERATOR = "operator";
    private static final String KEY_VALUE = "value";
    private static final String KEY_WHOLE_WORD = "whole_word";
    private static final int FORMAT_CONDITIONS = 1;
    private static final int SECTION_KEEP = 0;
    private static final int SECTION_SKIP = 1;
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

    private static final String CREATE_TABLE_EPISODE_FILTER_CONDITIONS = "CREATE TABLE IF NOT EXISTS "
            + TABLE_NAME_EPISODE_FILTER_CONDITIONS + " ("
            + KEY_FEED + " INTEGER NOT NULL, "
            + KEY_SECTION + " INTEGER NOT NULL, "
            + KEY_POSITION + " INTEGER NOT NULL, "
            + KEY_FIELD + " TEXT NOT NULL, "
            + KEY_OPERATOR + " TEXT NOT NULL, "
            + KEY_VALUE + " TEXT NOT NULL DEFAULT '', "
            + KEY_WHOLE_WORD + " INTEGER NOT NULL DEFAULT 0)";

    private final SQLiteDatabase db;

    EpisodeFilterDbAdapter(SQLiteDatabase db) {
        this.db = db;
    }

    static void createTables(SQLiteDatabase db) {
        if (db.isReadOnly()) {
            return;
        }
        db.execSQL(CREATE_TABLE_FEED_EPISODE_FILTERS);
        db.execSQL(CREATE_TABLE_EPISODE_FILTER_CONDITIONS);
        addColumnIfMissing(db, KEY_SKIP_MATCH_ALL);
        addColumnIfMissing(db, KEY_FORMAT);
    }

    private static void addColumnIfMissing(SQLiteDatabase db, String column) {
        try (Cursor cursor = db.rawQuery("PRAGMA table_info(" + TABLE_NAME_FEED_EPISODE_FILTERS + ")", null)) {
            while (cursor.moveToNext()) {
                if (column.equals(cursor.getString(cursor.getColumnIndexOrThrow("name")))) {
                    return;
                }
            }
        }
        db.execSQL("ALTER TABLE " + TABLE_NAME_FEED_EPISODE_FILTERS + " ADD COLUMN " + column
                + " INTEGER NOT NULL DEFAULT 0");
    }

    @Nullable
    AdvancedEpisodeFilter getFilter(long feedId) {
        try (Cursor cursor = db.query(TABLE_NAME_FEED_EPISODE_FILTERS, null, KEY_FEED + "=?",
                new String[]{String.valueOf(feedId)}, null, null, null)) {
            if (!cursor.moveToFirst()) {
                return null;
            }
            if (cursor.getInt(cursor.getColumnIndexOrThrow(KEY_FORMAT)) == FORMAT_CONDITIONS) {
                return new AdvancedEpisodeFilter(getConditions(feedId, SECTION_KEEP),
                        cursor.getInt(cursor.getColumnIndexOrThrow(KEY_MATCH_ALL)) != 0,
                        getConditions(feedId, SECTION_SKIP),
                        cursor.getInt(cursor.getColumnIndexOrThrow(KEY_SKIP_MATCH_ALL)) != 0);
            }
            return AdvancedEpisodeFilter.fromLegacy(
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

    @NonNull
    private List<EpisodeCondition> getConditions(long feedId, int section) {
        List<EpisodeCondition> conditions = new ArrayList<>();
        try (Cursor cursor = db.query(TABLE_NAME_EPISODE_FILTER_CONDITIONS, null,
                KEY_FEED + "=? AND " + KEY_SECTION + "=?",
                new String[]{String.valueOf(feedId), String.valueOf(section)}, null, null, KEY_POSITION)) {
            while (cursor.moveToNext()) {
                try {
                    conditions.add(new EpisodeCondition(
                            EpisodeCondition.Field.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(KEY_FIELD))),
                            EpisodeCondition.Operator.valueOf(
                                    cursor.getString(cursor.getColumnIndexOrThrow(KEY_OPERATOR))),
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_VALUE)),
                            cursor.getInt(cursor.getColumnIndexOrThrow(KEY_WHOLE_WORD)) != 0));
                } catch (IllegalArgumentException ignored) {
                    continue;
                }
            }
        }
        return conditions;
    }

    void setFilter(long feedId, @NonNull AdvancedEpisodeFilter filter) {
        db.beginTransactionNonExclusive();
        try {
            ContentValues values = new ContentValues();
            values.put(KEY_FEED, feedId);
            values.put(KEY_FORMAT, FORMAT_CONDITIONS);
            values.put(KEY_MATCH_ALL, filter.isKeepMatchAll() ? 1 : 0);
            values.put(KEY_SKIP_MATCH_ALL, filter.isSkipMatchAll() ? 1 : 0);
            db.insertWithOnConflict(TABLE_NAME_FEED_EPISODE_FILTERS, null, values,
                    SQLiteDatabase.CONFLICT_REPLACE);
            deleteConditions(feedId);
            insertConditions(feedId, SECTION_KEEP, filter.getKeepConditions());
            insertConditions(feedId, SECTION_SKIP, filter.getSkipConditions());
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void insertConditions(long feedId, int section, @NonNull List<EpisodeCondition> conditions) {
        for (int i = 0; i < conditions.size(); i++) {
            EpisodeCondition condition = conditions.get(i);
            ContentValues values = new ContentValues();
            values.put(KEY_FEED, feedId);
            values.put(KEY_SECTION, section);
            values.put(KEY_POSITION, i);
            values.put(KEY_FIELD, condition.getField().name());
            values.put(KEY_OPERATOR, condition.getOperator().name());
            values.put(KEY_VALUE, condition.getValue());
            values.put(KEY_WHOLE_WORD, condition.isWholeWord() ? 1 : 0);
            db.insert(TABLE_NAME_EPISODE_FILTER_CONDITIONS, null, values);
        }
    }

    private void deleteConditions(long feedId) {
        db.delete(TABLE_NAME_EPISODE_FILTER_CONDITIONS, KEY_FEED + "=?", new String[]{String.valueOf(feedId)});
    }

    void deleteFeed(long feedId) {
        db.delete(TABLE_NAME_FEED_EPISODE_FILTERS, KEY_FEED + "=?", new String[]{String.valueOf(feedId)});
        deleteConditions(feedId);
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
