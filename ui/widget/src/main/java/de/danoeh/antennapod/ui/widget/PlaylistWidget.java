package de.danoeh.antennapod.ui.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SizeF;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.playlist.Playlist;
import de.danoeh.antennapod.playback.base.PlayerStatus;
import de.danoeh.antennapod.storage.database.PlaylistPlayback;
import de.danoeh.antennapod.storage.database.PlaylistReader;
import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;
import de.danoeh.antennapod.ui.appstartintent.MainActivityStarter;
import de.danoeh.antennapod.ui.appstartintent.MediaButtonStarter;
import de.danoeh.antennapod.ui.appstartintent.PlaylistPlayStarter;

public class PlaylistWidget extends AppWidgetProvider {
    public static final int MAX_PLAYLISTS = 4;
    private static final String TAG = "PlaylistWidget";
    private static final String PREFS = "PlaylistWidget";
    private static final String KEY_PLAYLISTS = "playlists_";
    private static final String PLAYLISTS_FRAGMENT = "PlaylistsFragment";
    private static final int MIN_COMPACT_TILE_WIDTH_DP = 80;
    private static final int MIN_COMPACT_HEIGHT_DP = 76;
    private static final int MIN_WIDE_TILE_WIDTH_DP = 140;
    private static final int MIN_WIDE_HEIGHT_DP = 56;
    private static final long REFRESH_DELAY_MS = 500;

    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private static final AtomicBoolean refreshPending = new AtomicBoolean(false);
    private static volatile Boolean playing = null;
    private static volatile long lastActivePlaylist = PlaylistPlayback.NO_PLAYLIST;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        refresh(context);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId,
                                          Bundle newOptions) {
        refresh(context);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        for (int appWidgetId : appWidgetIds) {
            editor.remove(KEY_PLAYLISTS + appWidgetId);
        }
        editor.apply();
    }

    @NonNull
    public static List<Long> getPlaylistIds(@NonNull Context context, int appWidgetId) {
        List<Long> ids = new ArrayList<>();
        String value = getPrefs(context).getString(KEY_PLAYLISTS + appWidgetId, null);
        if (TextUtils.isEmpty(value)) {
            return ids;
        }
        for (String id : value.split(",")) {
            try {
                ids.add(Long.parseLong(id));
            } catch (NumberFormatException e) {
                Log.w(TAG, "Ignoring bad playlist id " + id);
            }
        }
        return ids;
    }

    public static boolean isConfigured(@NonNull Context context, int appWidgetId) {
        return getPrefs(context).contains(KEY_PLAYLISTS + appWidgetId);
    }

    public static void setPlaylistIds(@NonNull Context context, int appWidgetId, @NonNull List<Long> ids) {
        getPrefs(context).edit().putString(KEY_PLAYLISTS + appWidgetId, TextUtils.join(",", ids)).apply();
        refresh(context);
    }

    public static void onPlayerStatus(@NonNull Context context, @Nullable WidgetUpdater.WidgetState state) {
        if (state == null || (state.status != PlayerStatus.PLAYING && state.status != PlayerStatus.PAUSED
                && state.status != PlayerStatus.STOPPED && state.status != PlayerStatus.ERROR)) {
            return;
        }
        boolean nowPlaying = state.status == PlayerStatus.PLAYING;
        long active = PlaylistPlayback.getActivePlaylist(context);
        Boolean wasPlaying = playing;
        if (wasPlaying == null || wasPlaying != nowPlaying || active != lastActivePlaylist) {
            playing = nowPlaying;
            lastActivePlaylist = active;
            refresh(context);
        }
    }

    public static void refresh(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        if (refreshPending.compareAndSet(false, true)) {
            executor.schedule(() -> {
                refreshPending.set(false);
                try {
                    update(appContext);
                } catch (RuntimeException e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                }
            }, REFRESH_DELAY_MS, TimeUnit.MILLISECONDS);
        }
    }

    private static void update(@NonNull Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] widgetIds = manager.getAppWidgetIds(new ComponentName(context, PlaylistWidget.class));
        if (widgetIds == null || widgetIds.length == 0) {
            return;
        }
        List<Playlist> playlists = PlaylistReader.getPlaylists();
        Map<Long, Playlist> byId = new HashMap<>();
        for (Playlist playlist : playlists) {
            byId.put(playlist.getId(), playlist);
        }
        byId.put(PlaylistPlayStarter.ALL_PLAYLISTS, allPlaylists(context));
        Boolean knownPlaying = playing;
        boolean isPlaying = knownPlaying != null ? knownPlaying
                : PlaybackPreferences.getCurrentPlayerStatus() == PlaybackPreferences.PLAYER_STATUS_PLAYING;
        long active = PlaylistPlayback.getActivePlaylist(context);
        Map<Long, Integer> counts = new HashMap<>();

        for (int widgetId : widgetIds) {
            List<Playlist> shown = new ArrayList<>();
            for (long id : getPlaylistIds(context, widgetId)) {
                Playlist playlist = byId.get(id);
                if (playlist != null) {
                    shown.add(playlist);
                }
            }
            if (shown.isEmpty() && !isConfigured(context, widgetId)) {
                shown.addAll(playlists.subList(0, Math.min(MAX_PLAYLISTS, playlists.size())));
            }
            manager.updateAppWidget(widgetId, buildViews(context, manager.getAppWidgetOptions(widgetId),
                    shown, counts, isPlaying, active));
        }
    }

    @NonNull
    private static RemoteViews buildViews(@NonNull Context context, @Nullable Bundle options,
                                          @NonNull List<Playlist> shown, @NonNull Map<Long, Integer> counts,
                                          boolean isPlaying, long active) {
        for (Playlist playlist : shown) {
            if (!counts.containsKey(playlist.getId())) {
                counts.put(playlist.getId(), countEpisodes(playlist.getId()));
            }
        }
        if (shown.isEmpty()) {
            return buildViews(context, shown, counts, isPlaying, active, false);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Map<SizeF, RemoteViews> sizes = new ArrayMap<>();
            int allCompactWidth = shown.size() * MIN_COMPACT_TILE_WIDTH_DP;
            for (int tiles = 1; tiles <= shown.size(); tiles++) {
                List<Playlist> part = shown.subList(0, tiles);
                sizes.put(new SizeF(tiles * MIN_COMPACT_TILE_WIDTH_DP, MIN_COMPACT_HEIGHT_DP),
                        buildViews(context, part, counts, isPlaying, active, true));
                int wideWidth = tiles * MIN_WIDE_TILE_WIDTH_DP;
                if (tiles == shown.size() || wideWidth < allCompactWidth) {
                    sizes.put(new SizeF(wideWidth, MIN_WIDE_HEIGHT_DP),
                            buildViews(context, part, counts, isPlaying, active, false));
                }
            }
            return new RemoteViews(sizes);
        }
        int width = options == null ? 0 : options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        int height = options == null ? 0 : options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
        int tiles = shown.size();
        if (width > 0) {
            tiles = Math.max(1, Math.min(tiles, width / MIN_COMPACT_TILE_WIDTH_DP));
        }
        boolean compact = width > 0 && width / tiles < MIN_WIDE_TILE_WIDTH_DP && height >= MIN_COMPACT_HEIGHT_DP;
        return buildViews(context, shown.subList(0, tiles), counts, isPlaying, active, compact);
    }

    @NonNull
    private static RemoteViews buildViews(@NonNull Context context, @NonNull List<Playlist> shown,
                                          @NonNull Map<Long, Integer> counts, boolean isPlaying, long active,
                                          boolean compact) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.playlist_widget);
        views.removeAllViews(R.id.playlistWidgetTiles);
        if (shown.isEmpty()) {
            views.setViewVisibility(R.id.playlistWidgetTiles, View.GONE);
            views.setViewVisibility(R.id.playlistWidgetEmpty, View.VISIBLE);
            views.setOnClickPendingIntent(R.id.playlistWidgetEmpty, getOpenPlaylistsIntent(context));
            return views;
        }
        views.setViewVisibility(R.id.playlistWidgetTiles, View.VISIBLE);
        views.setViewVisibility(R.id.playlistWidgetEmpty, View.GONE);
        for (Playlist playlist : shown) {
            Integer count = counts.get(playlist.getId());
            boolean allTile = playlist.getId() == PlaylistPlayStarter.ALL_PLAYLISTS;
            boolean tilePlaying = isPlaying && (active == playlist.getId()
                    || (allTile && active != PlaylistPlayback.NO_PLAYLIST));
            views.addView(R.id.playlistWidgetTiles,
                    createTile(context, playlist, count == null ? 0 : count, tilePlaying, compact));
        }
        return views;
    }

    @NonNull
    private static RemoteViews createTile(@NonNull Context context, @NonNull Playlist playlist, int count,
                                          boolean tilePlaying, boolean compact) {
        int layout;
        if (compact) {
            layout = tilePlaying ? R.layout.playlist_widget_tile_compact_playing
                    : R.layout.playlist_widget_tile_compact;
        } else {
            layout = tilePlaying ? R.layout.playlist_widget_tile_playing : R.layout.playlist_widget_tile;
        }
        RemoteViews tile = new RemoteViews(context.getPackageName(), layout);
        String summary = count == 0 ? context.getString(R.string.playlist_episodes_empty_title)
                : context.getResources().getQuantityString(R.plurals.num_episodes, count, count);
        if (tilePlaying) {
            summary = context.getString(R.string.playlist_widget_playing, summary);
        }
        tile.setTextViewText(R.id.playlistWidgetName, playlist.getName());
        tile.setTextViewText(R.id.playlistWidgetSummary, summary);
        tile.setContentDescription(R.id.playlistWidgetTile, context.getString(tilePlaying
                ? R.string.playlist_widget_pause : R.string.playlist_widget_play, playlist.getName()));
        PendingIntent click;
        if (tilePlaying) {
            click = MediaButtonStarter.createPendingIntent(context, Player.COMMAND_PLAY_PAUSE);
        } else if (count > 0) {
            click = PlaylistPlayStarter.getPendingIntent(context, playlist.getId());
        } else {
            click = getOpenPlaylistsIntent(context);
        }
        tile.setOnClickPendingIntent(R.id.playlistWidgetTile, click);
        return tile;
    }

    @NonNull
    public static Playlist allPlaylists(@NonNull Context context) {
        return new Playlist(PlaylistPlayStarter.ALL_PLAYLISTS, context.getString(R.string.playlists_all),
                false, false, -1);
    }

    private static int countEpisodes(long playlistId) {
        if (playlistId != PlaylistPlayStarter.ALL_PLAYLISTS) {
            return PlaylistReader.getEpisodes(playlistId).size();
        }
        Set<Long> ids = new HashSet<>();
        for (Playlist playlist : PlaylistReader.getPlaylists()) {
            for (FeedItem item : PlaylistReader.getEpisodes(playlist.getId())) {
                ids.add(item.getId());
            }
        }
        return ids.size();
    }

    @NonNull
    private static PendingIntent getOpenPlaylistsIntent(@NonNull Context context) {
        return PendingIntent.getActivity(context, R.id.pending_intent_playlist_widget_open,
                new MainActivityStarter(context).withFragmentLoaded(PLAYLISTS_FRAGMENT)
                        .withClearBackStack().getIntent(),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static SharedPreferences getPrefs(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
