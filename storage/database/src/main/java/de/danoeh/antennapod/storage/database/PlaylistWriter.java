package de.danoeh.antennapod.storage.database;

import androidx.annotation.NonNull;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import de.danoeh.antennapod.event.PlaylistEvent;
import de.danoeh.antennapod.model.playlist.Playlist;
import de.danoeh.antennapod.model.playlist.PlaylistRule;

public final class PlaylistWriter {
    public static final long ALL_PLAYLISTS = 0;

    private static final ExecutorService dbExec = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "PlaylistDatabaseExecutor");
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });

    private PlaylistWriter() {
    }

    public static Future<Long> createPlaylist(@NonNull Playlist playlist) {
        return dbExec.submit(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance().open();
            long id = adapter.getPlaylistDbAdapter().insertPlaylist(playlist);
            adapter.close();
            EventBus.getDefault().post(new PlaylistEvent(id));
            return id;
        });
    }

    public static Future<?> updatePlaylist(@NonNull Playlist playlist) {
        return dbExec.submit(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance().open();
            adapter.getPlaylistDbAdapter().updatePlaylist(playlist);
            adapter.close();
            EventBus.getDefault().post(new PlaylistEvent(playlist.getId()));
        });
    }

    public static Future<?> deletePlaylist(long playlistId) {
        return dbExec.submit(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance().open();
            adapter.getPlaylistDbAdapter().deletePlaylist(playlistId);
            adapter.close();
            EventBus.getDefault().post(new PlaylistEvent(playlistId));
        });
    }

    public static Future<?> setPlaylistOrder(@NonNull List<Playlist> playlists) {
        final List<Long> ids = new ArrayList<>();
        for (Playlist playlist : playlists) {
            ids.add(playlist.getId());
        }
        return dbExec.submit(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance().open();
            adapter.getPlaylistDbAdapter().setPlaylistOrder(ids);
            adapter.close();
            EventBus.getDefault().post(new PlaylistEvent(ALL_PLAYLISTS));
        });
    }

    public static Future<Long> addRule(@NonNull PlaylistRule rule) {
        return dbExec.submit(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance().open();
            long id = adapter.getPlaylistDbAdapter().insertRule(rule);
            adapter.close();
            EventBus.getDefault().post(new PlaylistEvent(rule.getPlaylistId()));
            return id;
        });
    }

    public static Future<?> updateRule(@NonNull PlaylistRule rule) {
        return dbExec.submit(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance().open();
            adapter.getPlaylistDbAdapter().updateRule(rule);
            adapter.close();
            EventBus.getDefault().post(new PlaylistEvent(rule.getPlaylistId()));
        });
    }

    public static Future<?> deleteRule(@NonNull PlaylistRule rule) {
        return dbExec.submit(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance().open();
            adapter.getPlaylistDbAdapter().deleteRule(rule.getId());
            adapter.close();
            EventBus.getDefault().post(new PlaylistEvent(rule.getPlaylistId()));
        });
    }

    public static Future<?> setRuleOrder(long playlistId, @NonNull List<PlaylistRule> rules) {
        final List<Long> ids = new ArrayList<>();
        for (PlaylistRule rule : rules) {
            ids.add(rule.getId());
        }
        return dbExec.submit(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance().open();
            adapter.getPlaylistDbAdapter().setRuleOrder(ids);
            adapter.close();
            EventBus.getDefault().post(new PlaylistEvent(playlistId));
        });
    }
}
