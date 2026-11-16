package de.danoeh.antennapod.storage.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.model.playlist.Playlist;
import de.danoeh.antennapod.model.playlist.PlaylistRule;

public final class PlaylistReader {

    private PlaylistReader() {
    }

    @NonNull
    public static synchronized List<Playlist> getPlaylists() {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try {
            return adapter.getPlaylistDbAdapter().getPlaylists();
        } finally {
            adapter.close();
        }
    }

    @Nullable
    public static synchronized Playlist getPlaylist(long playlistId) {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try {
            return adapter.getPlaylistDbAdapter().getPlaylist(playlistId);
        } finally {
            adapter.close();
        }
    }

    @NonNull
    public static synchronized List<PlaylistRule> getRules(long playlistId) {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try {
            return adapter.getPlaylistDbAdapter().getRules(playlistId);
        } finally {
            adapter.close();
        }
    }

    @NonNull
    public static List<FeedItem> getEpisodes(long playlistId) {
        return getEpisodes(playlistId, Collections.emptySet());
    }

    @NonNull
    public static List<FeedItem> getAutoDownloadEpisodes() {
        List<FeedItem> episodes = new ArrayList<>();
        Set<Long> ids = new HashSet<>();
        Map<Long, Feed> feeds = loadFeeds();
        for (Playlist playlist : getPlaylists()) {
            if (playlist.isAutoDownload()) {
                for (FeedItem item : getEpisodes(limitForAutoDownload(getRules(playlist.getId())),
                        Collections.emptySet(), feeds)) {
                    if (ids.add(item.getId())) {
                        episodes.add(item);
                    }
                }
            }
        }
        return episodes;
    }

    @NonNull
    static List<PlaylistRule> limitForAutoDownload(@NonNull List<PlaylistRule> rules) {
        List<PlaylistRule> limited = new ArrayList<>();
        for (PlaylistRule rule : rules) {
            int count = rule.getEpisodeCount();
            if (count == PlaylistRule.ALL_EPISODES || count > PlaylistRule.MAX_AUTO_DOWNLOAD_EPISODES) {
                count = PlaylistRule.MAX_AUTO_DOWNLOAD_EPISODES;
            }
            limited.add(new PlaylistRule(rule.getId(), rule.getPlaylistId(), rule.getFeedId(), count,
                    rule.getPickFrom(), rule.getPlayOrder(), rule.getPriority(), rule.getPosition()));
        }
        return limited;
    }

    public static int getDownloadedEpisodeCountInPlaylists() {
        int count = 0;
        for (FeedItem item : getProtectedEpisodes().values()) {
            if (item.getMedia() != null && item.getMedia().isDownloaded()) {
                count++;
            }
        }
        return count;
    }

    @NonNull
    public static Set<Long> getEpisodeIdsInPlaylists() {
        return getProtectedEpisodes().keySet();
    }

    @NonNull
    private static Map<Long, FeedItem> getProtectedEpisodes() {
        Map<Long, FeedItem> episodes = new HashMap<>();
        Map<Long, Feed> feeds = loadFeeds();
        for (Playlist playlist : getPlaylists()) {
            for (FeedItem item : getEpisodes(limitForAutoDownload(getRules(playlist.getId())),
                    Collections.emptySet(), feeds)) {
                episodes.put(item.getId(), item);
            }
        }
        return episodes;
    }

    @NonNull
    public static List<FeedItem> getEpisodes(long playlistId, @NonNull final Set<Long> excludedOldestIds) {
        List<PlaylistRule> rules = getRules(playlistId);
        if (rules.isEmpty()) {
            return new ArrayList<>();
        }
        return getEpisodes(rules, excludedOldestIds, loadFeeds());
    }

    @NonNull
    private static Map<Long, Feed> loadFeeds() {
        Map<Long, Feed> feeds = new HashMap<>();
        for (Feed feed : DBReader.getFeedList()) {
            feeds.put(feed.getId(), feed);
        }
        return feeds;
    }

    @NonNull
    private static List<FeedItem> getEpisodes(@NonNull List<PlaylistRule> rules,
                                              @NonNull final Set<Long> excludedOldestIds,
                                              @NonNull final Map<Long, Feed> feeds) {
        if (rules.isEmpty()) {
            return new ArrayList<>();
        }
        return PlaylistEngine.computeEpisodes(rules, new PlaylistEngine.EpisodeSource() {
            @NonNull
            @Override
            public List<FeedItem> getNewest(long feedId, int limit) {
                Feed feed = feeds.get(feedId);
                if (feed == null) {
                    return new ArrayList<>();
                }
                return DBReader.getFeedItemList(feed, new FeedItemFilter(FeedItemFilter.HAS_MEDIA),
                        SortOrder.DATE_NEW_OLD, 0, limit);
            }

            @NonNull
            @Override
            public List<FeedItem> getOldestUnplayed(long feedId, int limit) {
                Feed feed = feeds.get(feedId);
                if (feed == null) {
                    return new ArrayList<>();
                }
                int fetchLimit = limit == Integer.MAX_VALUE ? limit : limit + excludedOldestIds.size();
                List<FeedItem> result = new ArrayList<>();
                for (FeedItem item : DBReader.getFeedItemList(feed,
                        new FeedItemFilter(FeedItemFilter.HAS_MEDIA, FeedItemFilter.UNPLAYED),
                        SortOrder.DATE_OLD_NEW, 0, fetchLimit)) {
                    if (result.size() < limit && !excludedOldestIds.contains(item.getId())) {
                        result.add(item);
                    }
                }
                return result;
            }
        });
    }
}
