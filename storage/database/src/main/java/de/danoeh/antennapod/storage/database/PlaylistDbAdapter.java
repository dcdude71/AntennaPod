package de.danoeh.antennapod.storage.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.danoeh.antennapod.model.playlist.Playlist;
import de.danoeh.antennapod.model.playlist.PlaylistRule;

public class PlaylistDbAdapter {
    public static final String TABLE_NAME_PLAYLISTS = "Playlists";
    public static final String TABLE_NAME_PLAYLIST_RULES = "PlaylistRules";
    public static final String TABLE_NAME_PLAYLIST_DISMISSED = "PlaylistDismissedEpisodes";
    public static final String TABLE_NAME_PLAYLIST_EPISODES = "PlaylistEpisodes";

    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "name";
    private static final String KEY_AUTO_DOWNLOAD = "auto_download";
    private static final String KEY_ANDROID_AUTO = "android_auto";
    private static final String KEY_POSITION = "position";
    private static final String KEY_DOWNLOADED_ONLY = "downloaded_only";
    private static final String KEY_PLAYLIST = "playlist";
    private static final String KEY_FEED = "feed";
    private static final String KEY_EPISODE_COUNT = "episode_count";
    private static final String KEY_PICK_FROM = "pick_from";
    private static final String KEY_PLAY_ORDER = "play_order";
    private static final String KEY_PRIORITY = "priority";
    private static final String KEY_ITEM = "item";
    private static final String KEY_MANUAL = "manual";
    private static final String KEY_KEEP_PLAYED = "keep_played";

    private static final String CREATE_TABLE_PLAYLISTS = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME_PLAYLISTS + " ("
            + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + KEY_NAME + " TEXT NOT NULL, "
            + KEY_AUTO_DOWNLOAD + " INTEGER NOT NULL DEFAULT 0, "
            + KEY_ANDROID_AUTO + " INTEGER NOT NULL DEFAULT 0, "
            + KEY_POSITION + " INTEGER NOT NULL DEFAULT 0, "
            + KEY_DOWNLOADED_ONLY + " INTEGER NOT NULL DEFAULT 0)";

    private static final String CREATE_TABLE_PLAYLIST_RULES = "CREATE TABLE IF NOT EXISTS "
            + TABLE_NAME_PLAYLIST_RULES + " ("
            + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + KEY_PLAYLIST + " INTEGER NOT NULL, "
            + KEY_FEED + " INTEGER NOT NULL, "
            + KEY_EPISODE_COUNT + " INTEGER NOT NULL DEFAULT 0, "
            + KEY_PICK_FROM + " INTEGER NOT NULL DEFAULT 0, "
            + KEY_PLAY_ORDER + " INTEGER NOT NULL DEFAULT 0, "
            + KEY_PRIORITY + " INTEGER NOT NULL DEFAULT 1, "
            + KEY_POSITION + " INTEGER NOT NULL DEFAULT 0)";

    private static final String CREATE_INDEX_PLAYLIST_RULES_PLAYLIST = "CREATE INDEX IF NOT EXISTS "
            + TABLE_NAME_PLAYLIST_RULES + "_" + KEY_PLAYLIST + " ON " + TABLE_NAME_PLAYLIST_RULES
            + " (" + KEY_PLAYLIST + ")";

    private final SQLiteDatabase db;

    PlaylistDbAdapter(SQLiteDatabase db) {
        this.db = db;
    }

    static void createTables(SQLiteDatabase db) {
        if (db.isReadOnly()) {
            return;
        }
        db.execSQL(CREATE_TABLE_PLAYLISTS);
        db.execSQL(CREATE_TABLE_PLAYLIST_RULES);
        db.execSQL(CREATE_INDEX_PLAYLIST_RULES_PLAYLIST);
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME_PLAYLIST_DISMISSED + " ("
                + KEY_ITEM + " INTEGER PRIMARY KEY)");
        addColumnIfMissing(db, TABLE_NAME_PLAYLISTS, KEY_DOWNLOADED_ONLY, "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing(db, TABLE_NAME_PLAYLISTS, KEY_MANUAL, "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing(db, TABLE_NAME_PLAYLISTS, KEY_KEEP_PLAYED, "INTEGER NOT NULL DEFAULT 0");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME_PLAYLIST_EPISODES + " ("
                + KEY_PLAYLIST + " INTEGER NOT NULL, "
                + KEY_ITEM + " INTEGER NOT NULL, "
                + KEY_POSITION + " INTEGER NOT NULL DEFAULT 0, "
                + "PRIMARY KEY (" + KEY_PLAYLIST + ", " + KEY_ITEM + "))");
    }

    private static void addColumnIfMissing(SQLiteDatabase db, String table, String column, String definition) {
        try (Cursor cursor = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            int nameIndex = cursor.getColumnIndexOrThrow("name");
            while (cursor.moveToNext()) {
                if (column.equals(cursor.getString(nameIndex))) {
                    return;
                }
            }
        }
        db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
    }

    @NonNull
    List<Playlist> getPlaylists() {
        List<Playlist> playlists = new ArrayList<>();
        try (Cursor cursor = db.query(TABLE_NAME_PLAYLISTS, null, null, null, null, null,
                KEY_POSITION + " ASC, " + KEY_ID + " ASC")) {
            while (cursor.moveToNext()) {
                playlists.add(playlistFromCursor(cursor));
            }
        }
        return playlists;
    }

    @Nullable
    Playlist getPlaylist(long playlistId) {
        try (Cursor cursor = db.query(TABLE_NAME_PLAYLISTS, null, KEY_ID + "=?",
                new String[]{String.valueOf(playlistId)}, null, null, null)) {
            return cursor.moveToFirst() ? playlistFromCursor(cursor) : null;
        }
    }

    long insertPlaylist(Playlist playlist) {
        playlist.setPosition(nextPosition(TABLE_NAME_PLAYLISTS, null));
        long id = db.insert(TABLE_NAME_PLAYLISTS, null, playlistToValues(playlist));
        playlist.setId(id);
        return id;
    }

    void updatePlaylist(Playlist playlist) {
        db.update(TABLE_NAME_PLAYLISTS, playlistToValues(playlist), KEY_ID + "=?",
                new String[]{String.valueOf(playlist.getId())});
    }

    void deletePlaylist(long playlistId) {
        try {
            db.beginTransactionNonExclusive();
            db.delete(TABLE_NAME_PLAYLIST_RULES, KEY_PLAYLIST + "=?", new String[]{String.valueOf(playlistId)});
            db.delete(TABLE_NAME_PLAYLIST_EPISODES, KEY_PLAYLIST + "=?", new String[]{String.valueOf(playlistId)});
            db.delete(TABLE_NAME_PLAYLISTS, KEY_ID + "=?", new String[]{String.valueOf(playlistId)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    void setPlaylistOrder(List<Long> playlistIds) {
        setOrder(TABLE_NAME_PLAYLISTS, playlistIds);
    }

    @NonNull
    List<PlaylistRule> getRules(long playlistId) {
        List<PlaylistRule> rules = new ArrayList<>();
        try (Cursor cursor = db.query(TABLE_NAME_PLAYLIST_RULES, null, KEY_PLAYLIST + "=?",
                new String[]{String.valueOf(playlistId)}, null, null, KEY_POSITION + " ASC, " + KEY_ID + " ASC")) {
            while (cursor.moveToNext()) {
                rules.add(ruleFromCursor(cursor));
            }
        }
        return rules;
    }

    long insertRule(PlaylistRule rule) {
        rule.setPosition(nextPosition(TABLE_NAME_PLAYLIST_RULES, rule.getPlaylistId()));
        long id = db.insert(TABLE_NAME_PLAYLIST_RULES, null, ruleToValues(rule));
        rule.setId(id);
        return id;
    }

    void updateRule(PlaylistRule rule) {
        db.update(TABLE_NAME_PLAYLIST_RULES, ruleToValues(rule), KEY_ID + "=?",
                new String[]{String.valueOf(rule.getId())});
    }

    void deleteRule(long ruleId) {
        db.delete(TABLE_NAME_PLAYLIST_RULES, KEY_ID + "=?", new String[]{String.valueOf(ruleId)});
    }

    void setRuleOrder(List<Long> ruleIds) {
        setOrder(TABLE_NAME_PLAYLIST_RULES, ruleIds);
    }

    void deleteRulesOfFeed(long feedId) {
        db.delete(TABLE_NAME_PLAYLIST_RULES, KEY_FEED + "=?", new String[]{String.valueOf(feedId)});
        db.delete(TABLE_NAME_PLAYLIST_EPISODES, KEY_ITEM + " NOT IN (SELECT " + PodDBAdapter.KEY_ID + " FROM "
                + PodDBAdapter.TABLE_NAME_FEED_ITEMS + ")", null);
    }

    @NonNull
    List<Long> getEpisodeIds(long playlistId) {
        List<Long> ids = new ArrayList<>();
        try (Cursor cursor = db.query(TABLE_NAME_PLAYLIST_EPISODES, new String[]{KEY_ITEM}, KEY_PLAYLIST + "=?",
                new String[]{String.valueOf(playlistId)}, null, null, KEY_POSITION + " ASC")) {
            while (cursor.moveToNext()) {
                ids.add(cursor.getLong(0));
            }
        }
        return ids;
    }

    @NonNull
    Set<Long> getPlaylistIdsContaining(long itemId) {
        Set<Long> ids = new HashSet<>();
        try (Cursor cursor = db.query(TABLE_NAME_PLAYLIST_EPISODES, new String[]{KEY_PLAYLIST}, KEY_ITEM + "=?",
                new String[]{String.valueOf(itemId)}, null, null, null)) {
            while (cursor.moveToNext()) {
                ids.add(cursor.getLong(0));
            }
        }
        return ids;
    }

    void addEpisodes(long playlistId, List<Long> itemIds) {
        List<Long> existing = getEpisodeIds(playlistId);
        List<Long> order = new ArrayList<>(existing);
        for (long itemId : itemIds) {
            if (!order.contains(itemId)) {
                order.add(itemId);
            }
        }
        setEpisodes(playlistId, order);
    }

    void addEpisodeAt(long playlistId, long itemId, int position) {
        List<Long> order = getEpisodeIds(playlistId);
        order.remove(Long.valueOf(itemId));
        order.add(Math.max(0, Math.min(position, order.size())), itemId);
        setEpisodes(playlistId, order);
    }

    void removeEpisode(long playlistId, long itemId) {
        db.delete(TABLE_NAME_PLAYLIST_EPISODES, KEY_PLAYLIST + "=? AND " + KEY_ITEM + "=?",
                new String[]{String.valueOf(playlistId), String.valueOf(itemId)});
    }

    void setEpisodes(long playlistId, List<Long> itemIds) {
        try {
            db.beginTransactionNonExclusive();
            db.delete(TABLE_NAME_PLAYLIST_EPISODES, KEY_PLAYLIST + "=?", new String[]{String.valueOf(playlistId)});
            ContentValues values = new ContentValues();
            for (int i = 0; i < itemIds.size(); i++) {
                values.put(KEY_PLAYLIST, playlistId);
                values.put(KEY_ITEM, itemIds.get(i));
                values.put(KEY_POSITION, i);
                db.insertWithOnConflict(TABLE_NAME_PLAYLIST_EPISODES, null, values, SQLiteDatabase.CONFLICT_IGNORE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    void deleteAll() {
        db.delete(TABLE_NAME_PLAYLIST_RULES, null, null);
        db.delete(TABLE_NAME_PLAYLISTS, null, null);
        db.delete(TABLE_NAME_PLAYLIST_DISMISSED, null, null);
        db.delete(TABLE_NAME_PLAYLIST_EPISODES, null, null);
    }

    void addDismissedEpisode(long itemId) {
        ContentValues values = new ContentValues();
        values.put(KEY_ITEM, itemId);
        db.insertWithOnConflict(TABLE_NAME_PLAYLIST_DISMISSED, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    Set<Long> getDismissedEpisodes() {
        Set<Long> ids = new HashSet<>();
        try (Cursor cursor = db.query(TABLE_NAME_PLAYLIST_DISMISSED, new String[]{KEY_ITEM},
                null, null, null, null, null)) {
            while (cursor.moveToNext()) {
                ids.add(cursor.getLong(0));
            }
        }
        return ids;
    }

    private void setOrder(String table, List<Long> ids) {
        try {
            db.beginTransactionNonExclusive();
            ContentValues values = new ContentValues();
            for (int i = 0; i < ids.size(); i++) {
                values.put(KEY_POSITION, i);
                db.update(table, values, KEY_ID + "=?", new String[]{String.valueOf(ids.get(i))});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private int nextPosition(String table, @Nullable Long playlistId) {
        String where = playlistId == null ? "" : " WHERE " + KEY_PLAYLIST + "=?";
        String[] args = playlistId == null ? null : new String[]{String.valueOf(playlistId)};
        try (Cursor cursor = db.rawQuery("SELECT MAX(" + KEY_POSITION + ") FROM " + table + where, args)) {
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                return cursor.getInt(0) + 1;
            }
            return 0;
        }
    }

    private static ContentValues playlistToValues(Playlist playlist) {
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, playlist.getName());
        values.put(KEY_AUTO_DOWNLOAD, playlist.isAutoDownload() ? 1 : 0);
        values.put(KEY_ANDROID_AUTO, playlist.isShowInAndroidAuto() ? 1 : 0);
        values.put(KEY_POSITION, playlist.getPosition());
        values.put(KEY_DOWNLOADED_ONLY, playlist.isDownloadedOnly() ? 1 : 0);
        values.put(KEY_MANUAL, playlist.isManual() ? 1 : 0);
        values.put(KEY_KEEP_PLAYED, playlist.isKeepPlayed() ? 1 : 0);
        return values;
    }

    private static Playlist playlistFromCursor(Cursor cursor) {
        Playlist playlist = new Playlist(
                cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME)),
                cursor.getInt(cursor.getColumnIndexOrThrow(KEY_AUTO_DOWNLOAD)) != 0,
                cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ANDROID_AUTO)) != 0,
                cursor.getInt(cursor.getColumnIndexOrThrow(KEY_POSITION)));
        playlist.setDownloadedOnly(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_DOWNLOADED_ONLY)) != 0);
        playlist.setManual(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_MANUAL)) != 0);
        playlist.setKeepPlayed(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_KEEP_PLAYED)) != 0);
        return playlist;
    }

    private static ContentValues ruleToValues(PlaylistRule rule) {
        ContentValues values = new ContentValues();
        values.put(KEY_PLAYLIST, rule.getPlaylistId());
        values.put(KEY_FEED, rule.getFeedId());
        values.put(KEY_EPISODE_COUNT, rule.getEpisodeCount());
        values.put(KEY_PICK_FROM, rule.getPickFrom().code);
        values.put(KEY_PLAY_ORDER, rule.getPlayOrder().code);
        values.put(KEY_PRIORITY, rule.getPriority());
        values.put(KEY_POSITION, rule.getPosition());
        return values;
    }

    private static PlaylistRule ruleFromCursor(Cursor cursor) {
        return new PlaylistRule(
                cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)),
                cursor.getLong(cursor.getColumnIndexOrThrow(KEY_PLAYLIST)),
                cursor.getLong(cursor.getColumnIndexOrThrow(KEY_FEED)),
                cursor.getInt(cursor.getColumnIndexOrThrow(KEY_EPISODE_COUNT)),
                PlaylistRule.PickFrom.fromCode(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_PICK_FROM))),
                PlaylistRule.PlayOrder.fromCode(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_PLAY_ORDER))),
                cursor.getInt(cursor.getColumnIndexOrThrow(KEY_PRIORITY)),
                cursor.getInt(cursor.getColumnIndexOrThrow(KEY_POSITION)));
    }
}
