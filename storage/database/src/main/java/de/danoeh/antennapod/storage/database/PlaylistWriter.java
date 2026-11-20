package de.danoeh.antennapod.storage.database;

import androidx.annotation.NonNull;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import de.danoeh.antennapod.event.PlaylistEvent;
import de.danoeh.antennapod.model.feed.FeedItem;
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
            long id;
            try {
                id = adapter.getPlaylistDbAdapter().insertPlaylist(playlist);
            } finally {
                adapter.close();
            }
            EventBus.getDefault().post(new PlaylistEvent(id));
            return id;
        });
    }

    public static Future<Long> duplicatePlaylist(long playlistId, @NonNull String name) {
        return dbExec.submit(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance().open();
            long id;
            try {
                PlaylistDbAdapter playlists = adapter.getPlaylistDbAdapter();
                Playlist original = playlists.getPlaylist(playlistId);
                if (original == null) {
                    return PlaylistPlayback.NO_PLAYLIST;
                }
                Playlist copy = new Playlist(name);
                copy.setAutoDownload(original.isAutoDownload());
                copy.setShowInAndroidAuto(original.isShowInAndroidAuto());
                copy.setDownloadedOnly(original.isDownloadedOnly());
                copy.setManual(original.isManual());
                copy.setKeepPlayed(original.isKeepPlayed());
                id = playlists.insertPlaylist(copy);
                playlists.setEpisodes(id, playlists.getEpisodeIds(playlistId));
                for (PlaylistRule rule : playlists.getRules(playlistId)) {
                    playlists.insertRule(new PlaylistRule(0, id, rule.getFeedId(), rule.getEpisodeCount(),
                            rule.getPickFrom(), rule.getPlayOrder(), rule.getPriority(), 0));
                }
            } finally {
                adapter.close();
            }
            EventBus.getDefault().post(new PlaylistEvent(id));
            return id;
        });
    }

    public static Future<Long> convertToManual(long playlistId, @NonNull String name) {
        return dbExec.submit(() -> {
            Playlist original = PlaylistReader.getPlaylist(playlistId);
            if (original == null) {
                return PlaylistPlayback.NO_PLAYLIST;
            }
            List<Long> itemIds = new ArrayList<>();
            for (FeedItem item : PlaylistReader.getEpisodes(playlistId)) {
                itemIds.add(item.getId());
            }
            PodDBAdapter adapter = PodDBAdapter.getInstance().open();
            long id;
            try {
                PlaylistDbAdapter playlists = adapter.getPlaylistDbAdapter();
                Playlist copy = new Playlist(name);
                copy.setManual(true);
                copy.setShowInAndroidAuto(original.isShowInAndroidAuto());
                id = playlists.insertPlaylist(copy);
                playlists.setEpisodes(id, itemIds);
            } finally {
                adapter.close();
            }
            EventBus.getDefault().post(new PlaylistEvent(id));
            return id;
        });
    }

    public static Future<?> addEpisodes(long playlistId, @NonNull List<FeedItem> items) {
        final List<Long> itemIds = new ArrayList<>();
        for (FeedItem item : items) {
            itemIds.add(item.getId());
        }
        return dbExec.submit(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance().open();
            try {
                adapter.getPlaylistDbAdapter().addEpisodes(playlistId, itemIds);
            } finally {
                adapter.close();
            }
            EventBus.getDefault().post(new PlaylistEvent(playlistId));
        });
    }

    public static Future<?> setMembership(@NonNull List<FeedItem> items, @NonNull Collection<Long> addTo,
                                          @NonNull Collection<Long> removeFrom) {
        final List<Long> itemIds = new ArrayList<>();
        for (FeedItem item : items) {
            itemIds.add(item.getId());
        }
        return dbExec.submit(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance().open();
            try {
                PlaylistDbAdapter playlists = adapter.getPlaylistDbAdapter();
                for (long playlistId : addTo) {
                    playlists.addEpisodes(playlistId, itemIds);
                }
                for (long playlistId : removeFrom) {
                    for (long itemId : itemIds) {
                        playlists.removeEpisode(playlistId, itemId);
                    }
                }
            } finally {
                adapter.close();
            }
            EventBus.getDefault().post(new PlaylistEvent(ALL_PLAYLISTS));
        });
    }

    public static Future<Integer> removeEpisode(long playlistId, @NonNull FeedItem item) {
        final long itemId = item.getId();
        return dbExec.submit(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance().open();
            int position;
            try {
                PlaylistDbAdapter playlists = adapter.getPlaylistDbAdapter();
                position = playlists.getEpisodeIds(playlistId).indexOf(itemId);
                playlists.removeEpisode(playlistId, itemId);
            } finally {
                adapter.close();
            }
            EventBus.getDefault().post(new PlaylistEvent(playlistId));
            return position;
        });
    }

    public static Future<?> addEpisodeAt(long playlistId, @NonNull FeedItem item, int position) {
        final long itemId = item.getId();
        return dbExec.submit(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance().open();
            try {
                adapter.getPlaylistDbAdapter().addEpisodeAt(playlistId, itemId, position);
            } finally {
                adapter.close();
            }
            EventBus.getDefault().post(new PlaylistEvent(playlistId));
        });
    }

    public static Future<?> setEpisodeOrder(long playlistId, @NonNull List<FeedItem> items) {
        final List<Long> visibleIds = new ArrayList<>();
        for (FeedItem item : items) {
            visibleIds.add(item.getId());
        }
        return dbExec.submit(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance().open();
            try {
                PlaylistDbAdapter playlists = adapter.getPlaylistDbAdapter();
                List<Long> order = new ArrayList<>(visibleIds);
                for (long itemId : playlists.getEpisodeIds(playlistId)) {
                    if (!order.contains(itemId)) {
                        order.add(itemId);
                    }
                }
                playlists.setEpisodes(playlistId, order);
            } finally {
                adapter.close();
            }
            EventBus.getDefault().post(new PlaylistEvent(playlistId));
        });
    }

    public static Future<?> updatePlaylist(@NonNull Playlist playlist) {
        return dbExec.submit(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance().open();
            try {
                adapter.getPlaylistDbAdapter().updatePlaylist(playlist);
            } finally {
                adapter.close();
            }
            EventBus.getDefault().post(new PlaylistEvent(playlist.getId()));
        });
    }

    public static Future<?> deletePlaylist(long playlistId) {
        return dbExec.submit(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance().open();
            try {
                adapter.getPlaylistDbAdapter().deletePlaylist(playlistId);
            } finally {
                adapter.close();
            }
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
            try {
                adapter.getPlaylistDbAdapter().setPlaylistOrder(ids);
            } finally {
                adapter.close();
            }
            EventBus.getDefault().post(new PlaylistEvent(ALL_PLAYLISTS));
        });
    }

    public static Future<Long> addRule(@NonNull PlaylistRule rule) {
        return dbExec.submit(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance().open();
            long id;
            try {
                id = adapter.getPlaylistDbAdapter().insertRule(rule);
            } finally {
                adapter.close();
            }
            EventBus.getDefault().post(new PlaylistEvent(rule.getPlaylistId()));
            return id;
        });
    }

    public static Future<?> updateRule(@NonNull PlaylistRule rule) {
        return dbExec.submit(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance().open();
            try {
                adapter.getPlaylistDbAdapter().updateRule(rule);
            } finally {
                adapter.close();
            }
            EventBus.getDefault().post(new PlaylistEvent(rule.getPlaylistId()));
        });
    }

    public static Future<?> deleteRule(@NonNull PlaylistRule rule) {
        return dbExec.submit(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance().open();
            try {
                adapter.getPlaylistDbAdapter().deleteRule(rule.getId());
            } finally {
                adapter.close();
            }
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
            try {
                adapter.getPlaylistDbAdapter().setRuleOrder(ids);
            } finally {
                adapter.close();
            }
            EventBus.getDefault().post(new PlaylistEvent(playlistId));
        });
    }

    public static Future<?> dismissEpisode(@NonNull FeedItem item) {
        final long itemId = item.getId();
        return dbExec.submit(() -> {
            PodDBAdapter adapter = PodDBAdapter.getInstance().open();
            try {
                adapter.getPlaylistDbAdapter().addDismissedEpisode(itemId);
            } finally {
                adapter.close();
            }
        });
    }
}
