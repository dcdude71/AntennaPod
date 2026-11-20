package de.danoeh.antennapod;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.media3.common.C;

import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.playlist.Playlist;
import de.danoeh.antennapod.net.sync.wearinterface.CastlistWearAdSkip;
import de.danoeh.antennapod.net.sync.wearinterface.CastlistWearPlaylists;
import de.danoeh.antennapod.playback.service.PlaybackController;
import de.danoeh.antennapod.playback.service.PlaybackServiceStarter;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.FeedAdSkip;
import de.danoeh.antennapod.storage.database.PlaylistPlayback;
import de.danoeh.antennapod.storage.database.PlaylistReader;
import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;

final class CastlistWearHandler {
    private static final String TAG = "CastlistWearHandler";

    private CastlistWearHandler() {
    }

    static boolean handle(@NonNull Context context, @NonNull String path, @NonNull String sourceNodeId) {
        if (CastlistWearPlaylists.PLAYLISTS.equals(path)) {
            byte[] payload = CastlistWearPlaylists.toBytes(listPlaylists(context));
            Wearable.getMessageClient(context).sendMessage(sourceNodeId, path, payload)
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to send playlists", e));
            return true;
        }
        Long adSkipItemId = CastlistWearAdSkip.parseAdSkipPath(path);
        if (adSkipItemId != null) {
            FeedItem item = DBReader.getFeedItem(adSkipItemId);
            int seconds = item == null ? 0 : FeedAdSkip.getSeconds(item.getFeedId());
            byte[] payload = CastlistWearAdSkip.secondsToBytes(seconds);
            Wearable.getMessageClient(context).sendMessage(sourceNodeId, path, payload)
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to send ad skip time", e));
            return true;
        }
        if (CastlistWearAdSkip.SKIP_AD.equals(path)) {
            skipAd(context);
            return true;
        }
        Long playlistId = CastlistWearPlaylists.parsePlayPlaylistPath(path);
        if (playlistId != null) {
            playPlaylist(context, playlistId);
            return true;
        }
        return path.startsWith(CastlistWearPlaylists.PLAY_PLAYLIST_PREFIX);
    }

    private static void skipAd(@NonNull Context context) {
        FeedMedia media = DBReader.getFeedMedia(PlaybackPreferences.getCurrentlyPlayingFeedMediaId());
        if (media == null || media.getItem() == null) {
            return;
        }
        final long skipMillis = FeedAdSkip.getSeconds(media.getItem().getFeedId()) * 1000L;
        if (skipMillis <= 0) {
            return;
        }
        PlaybackController.bindToMedia3Service(context.getApplicationContext(), controller -> {
            if (controller.getCurrentMediaItem() == null) {
                return;
            }
            long target = controller.getCurrentPosition() + skipMillis;
            long duration = controller.getDuration();
            controller.seekTo(duration == C.TIME_UNSET ? target : Math.min(duration, target));
        });
    }

    @NonNull
    private static List<CastlistWearPlaylists.Entry> listPlaylists(@NonNull Context context) {
        List<CastlistWearPlaylists.Entry> entries = new ArrayList<>();
        Map<Long, FeedItem> all = new HashMap<>();
        for (Playlist playlist : PlaylistReader.getPlaylists()) {
            List<FeedItem> episodes = PlaylistReader.getEpisodes(playlist.getId());
            for (FeedItem item : episodes) {
                all.put(item.getId(), item);
            }
            entries.add(new CastlistWearPlaylists.Entry(playlist.getId(), playlist.getName(),
                    playlist.isManual() ? CastlistWearPlaylists.KIND_MANUAL : CastlistWearPlaylists.KIND_SMART,
                    episodes.size(), PlaylistReader.getTimeLeftMillis(episodes)));
        }
        if (!entries.isEmpty()) {
            entries.add(0, new CastlistWearPlaylists.Entry(CastlistWearPlaylists.ALL_PLAYLISTS,
                    context.getString(R.string.playlists_all), CastlistWearPlaylists.KIND_ALL, all.size(),
                    PlaylistReader.getTimeLeftMillis(new ArrayList<>(all.values()))));
        }
        return entries;
    }

    private static void playPlaylist(@NonNull Context context, long requestedId) {
        long target = requestedId;
        FeedItem first = null;
        if (requestedId == CastlistWearPlaylists.ALL_PLAYLISTS) {
            for (Playlist playlist : PlaylistReader.getPlaylists()) {
                List<FeedItem> episodes = PlaylistReader.getEpisodes(playlist.getId());
                if (!episodes.isEmpty() && episodes.get(0).getMedia() != null) {
                    target = playlist.getId();
                    first = episodes.get(0);
                    break;
                }
            }
        } else {
            List<FeedItem> episodes = PlaylistReader.getEpisodes(requestedId);
            if (!episodes.isEmpty() && episodes.get(0).getMedia() != null) {
                first = episodes.get(0);
            }
        }
        if (first == null) {
            Log.d(TAG, "Nothing to play in playlist " + requestedId);
            return;
        }
        Context appContext = context.getApplicationContext();
        PlaylistPlayback.start(appContext, target);
        new PlaybackServiceStarter(appContext, first.getMedia())
                .callEvenIfRunning(true)
                .start();
    }
}
