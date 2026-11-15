package de.danoeh.antennapod.storage.database;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.danoeh.antennapod.model.feed.FeedItem;

public final class PlaylistPlayback {
    public static final long NO_PLAYLIST = 0;
    private static final String PREF_NAME = "PlaylistPlayback";
    private static final String PREF_ACTIVE_PLAYLIST = "activePlaylist";

    private static final Set<Long> skippedItemIds = Collections.synchronizedSet(new HashSet<>());

    private PlaylistPlayback() {
    }

    public static void start(@NonNull Context context, long playlistId) {
        skippedItemIds.clear();
        getPrefs(context).edit().putLong(PREF_ACTIVE_PLAYLIST, playlistId).apply();
    }

    public static void stop(@NonNull Context context) {
        skippedItemIds.clear();
        getPrefs(context).edit().putLong(PREF_ACTIVE_PLAYLIST, NO_PLAYLIST).apply();
    }

    public static long getActivePlaylist(@NonNull Context context) {
        return getPrefs(context).getLong(PREF_ACTIVE_PLAYLIST, NO_PLAYLIST);
    }

    public static boolean isPlayingFromPlaylist(@NonNull Context context, @NonNull FeedItem item) {
        long playlistId = getActivePlaylist(context);
        if (playlistId == NO_PLAYLIST) {
            return false;
        }
        for (FeedItem episode : PlaylistReader.getEpisodes(playlistId)) {
            if (episode.getId() == item.getId()) {
                return true;
            }
        }
        stop(context);
        return false;
    }

    @Nullable
    public static FeedItem getNextItem(@NonNull Context context, @NonNull FeedItem current, boolean skipped) {
        long playlistId = getActivePlaylist(context);
        if (playlistId == NO_PLAYLIST) {
            return DBReader.getNextInQueue(current);
        }
        if (skipped) {
            skippedItemIds.add(current.getId());
        }
        Set<Long> skippedIds;
        synchronized (skippedItemIds) {
            skippedIds = new HashSet<>(skippedItemIds);
        }
        FeedItem next = getNextEpisode(PlaylistReader.getEpisodes(playlistId, skippedIds),
                current.getId(), skippedIds);
        if (next == null) {
            stop(context);
        }
        return next;
    }

    @Nullable
    static FeedItem getNextEpisode(@NonNull List<FeedItem> episodes, long currentId, @NonNull Set<Long> skippedIds) {
        for (FeedItem episode : episodes) {
            if (episode.getId() != currentId && !skippedIds.contains(episode.getId())) {
                return episode;
            }
        }
        return null;
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}
