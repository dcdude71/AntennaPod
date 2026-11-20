package de.danoeh.antennapod.net.sync.wearinterface;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class CastlistWearPlaylists {
    private static final String TAG = "CastlistWearPlaylists";
    public static final String PLAYLISTS = "/castlist/playlists";
    public static final String PLAY_PLAYLIST_PREFIX = "/castlist/play_playlist/";
    public static final long ALL_PLAYLISTS = -1;
    public static final int KIND_ALL = 0;
    public static final int KIND_SMART = 1;
    public static final int KIND_MANUAL = 2;

    private CastlistWearPlaylists() {
    }

    public static final class Entry {
        public final long id;
        public final String name;
        public final int kind;
        public final int episodes;
        public final long timeLeftMillis;

        public Entry(long id, @NonNull String name, int kind, int episodes, long timeLeftMillis) {
            this.id = id;
            this.name = name;
            this.kind = kind;
            this.episodes = episodes;
            this.timeLeftMillis = timeLeftMillis;
        }
    }

    @NonNull
    public static String playPlaylistPath(long playlistId) {
        return PLAY_PLAYLIST_PREFIX + playlistId;
    }

    @Nullable
    public static Long parsePlayPlaylistPath(@NonNull String path) {
        if (!path.startsWith(PLAY_PLAYLIST_PREFIX)) {
            return null;
        }
        try {
            return Long.parseLong(path.substring(PLAY_PLAYLIST_PREFIX.length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @NonNull
    public static byte[] toBytes(@NonNull List<Entry> entries) {
        JSONArray array = new JSONArray();
        try {
            for (Entry entry : entries) {
                JSONObject object = new JSONObject();
                object.put("id", entry.id);
                object.put("name", entry.name);
                object.put("kind", entry.kind);
                object.put("episodes", entry.episodes);
                object.put("timeLeft", entry.timeLeftMillis);
                array.put(object);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to encode playlists", e);
        }
        return array.toString().getBytes(StandardCharsets.UTF_8);
    }

    @NonNull
    public static List<Entry> fromBytes(@Nullable byte[] bytes) {
        List<Entry> entries = new ArrayList<>();
        if (bytes == null) {
            return entries;
        }
        try {
            JSONArray array = new JSONArray(new String(bytes, StandardCharsets.UTF_8));
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                entries.add(new Entry(object.getLong("id"), object.getString("name"), object.getInt("kind"),
                        object.optInt("episodes"), object.optLong("timeLeft")));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to decode playlists", e);
        }
        return entries;
    }
}
