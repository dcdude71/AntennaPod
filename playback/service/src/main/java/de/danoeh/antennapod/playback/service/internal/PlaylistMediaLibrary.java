package de.danoeh.antennapod.playback.service.internal;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.media3.common.MediaItem;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.model.playlist.Playlist;
import de.danoeh.antennapod.playback.base.MediaItemAdapter;
import de.danoeh.antennapod.playback.service.R;
import de.danoeh.antennapod.storage.database.PlaylistPlayback;
import de.danoeh.antennapod.storage.database.PlaylistReader;

public final class PlaylistMediaLibrary {
    public static final String MEDIA_ID_PLAYLISTS = "playlists";
    static final String PLAYLIST_PREFIX = "Playlist:";
    static final String PLAYLIST_EPISODE_PREFIX = "PlaylistEpisode:";

    private PlaylistMediaLibrary() {
    }

    public static boolean isBrowsableId(@NonNull String mediaId) {
        return MEDIA_ID_PLAYLISTS.equals(mediaId) || mediaId.startsWith(PLAYLIST_PREFIX);
    }

    @WorkerThread
    @Nullable
    public static MediaItem createRootItem(@NonNull Context context) {
        int count = getVisiblePlaylists().size();
        return count == 0 ? null : createPlaylistsItem(context, count);
    }

    @WorkerThread
    @NonNull
    public static MediaItem createBrowsableItem(@NonNull Context context, @NonNull String mediaId) {
        if (MEDIA_ID_PLAYLISTS.equals(mediaId)) {
            return createPlaylistsItem(context, getVisiblePlaylists().size());
        }
        Playlist playlist = PlaylistReader.getPlaylist(parsePlaylistId(mediaId));
        if (playlist == null) {
            throw new IllegalArgumentException("Playlist not found: " + mediaId);
        }
        return createPlaylistItem(context, playlist);
    }

    @WorkerThread
    @NonNull
    public static ImmutableList<MediaItem> getChildren(@NonNull Context context, @NonNull String parentId) {
        ImmutableList.Builder<MediaItem> builder = ImmutableList.builder();
        if (MEDIA_ID_PLAYLISTS.equals(parentId)) {
            for (Playlist playlist : getVisiblePlaylists()) {
                builder.add(createPlaylistItem(context, playlist));
            }
            return builder.build();
        }
        long playlistId = parsePlaylistId(parentId);
        for (MediaItem item : MediaItemAdapter.fromItemList(context, PlaylistReader.getEpisodes(playlistId))) {
            builder.add(item.buildUpon().setMediaId(toEpisodeMediaId(playlistId, item.mediaId)).build());
        }
        return builder.build();
    }

    @WorkerThread
    @NonNull
    public static String resolveMediaId(@NonNull Context context, @NonNull String mediaId) {
        if (!mediaId.startsWith(PLAYLIST_EPISODE_PREFIX)) {
            return mediaId;
        }
        String[] parts = mediaId.substring(PLAYLIST_EPISODE_PREFIX.length()).split(":");
        if (parts.length != 2) {
            throw new NumberFormatException("Invalid playlist episode ID: " + mediaId);
        }
        long playlistId = Long.parseLong(parts[0]);
        Long.parseLong(parts[1]);
        PlaylistPlayback.start(context, playlistId);
        return parts[1];
    }

    @NonNull
    static String toEpisodeMediaId(long playlistId, @NonNull String mediaId) {
        return PLAYLIST_EPISODE_PREFIX + playlistId + ":" + mediaId;
    }

    static long parsePlaylistId(@NonNull String mediaId) {
        return Long.parseLong(mediaId.substring(PLAYLIST_PREFIX.length()));
    }

    @NonNull
    private static MediaItem createPlaylistsItem(@NonNull Context context, int count) {
        return MediaItemAdapter.from(context, MEDIA_ID_PLAYLISTS, context.getString(R.string.playlists_label),
                R.drawable.ic_playlists_black, context.getResources().getQuantityString(
                        R.plurals.num_playlists, count, count));
    }

    @NonNull
    private static MediaItem createPlaylistItem(@NonNull Context context, @NonNull Playlist playlist) {
        return MediaItemAdapter.from(context, PLAYLIST_PREFIX + playlist.getId(), playlist.getName(),
                R.drawable.ic_playlists_black, null);
    }

    @NonNull
    private static List<Playlist> getVisiblePlaylists() {
        List<Playlist> visible = new ArrayList<>();
        for (Playlist playlist : PlaylistReader.getPlaylists()) {
            if (playlist.isShowInAndroidAuto()) {
                visible.add(playlist);
            }
        }
        return visible;
    }
}
