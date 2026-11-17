package de.danoeh.antennapod.storage.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.greenrobot.eventbus.EventBus;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import de.danoeh.antennapod.event.PlaylistEvent;
import de.danoeh.antennapod.model.feed.AdvancedEpisodeFilter;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedFilter;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.storage.preferences.EpisodeFilterPreferences;

public final class EpisodeFilters {

    private EpisodeFilters() {
    }

    @Nullable
    public static synchronized AdvancedEpisodeFilter getAdvancedFilter(long feedId) {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try {
            return adapter.getEpisodeFilterDbAdapter().getFilter(feedId);
        } finally {
            adapter.close();
        }
    }

    public static synchronized void setAdvancedFilter(long feedId, @NonNull AdvancedEpisodeFilter filter) {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try {
            adapter.getEpisodeFilterDbAdapter().setFilter(feedId, filter);
        } finally {
            adapter.close();
        }
        EventBus.getDefault().post(new PlaylistEvent(PlaylistWriter.ALL_PLAYLISTS));
    }

    @NonNull
    public static AdvancedEpisodeFilter getAdvancedFilterOrBasic(@NonNull Feed feed) {
        AdvancedEpisodeFilter advanced = getAdvancedFilter(feed.getId());
        if (advanced != null) {
            return advanced;
        }
        return AdvancedEpisodeFilter.fromBasic(
                feed.getPreferences() != null ? feed.getPreferences().getFilter() : null);
    }

    public static boolean shouldAutoDownload(@NonNull FeedItem item) {
        Feed feed = item.getFeed();
        if (!EpisodeFilterPreferences.isAdvanced() || feed == null) {
            return feed == null || feed.getPreferences() == null
                    || feed.getPreferences().getFilter().shouldAutoDownload(item);
        }
        return filter(feed, Collections.singletonList(item)).size() == 1;
    }

    @NonNull
    public static List<FeedItem> filter(@NonNull Feed feed, @NonNull List<FeedItem> items) {
        if (items.isEmpty()) {
            return items;
        }
        if (!EpisodeFilterPreferences.isAdvanced()) {
            FeedFilter basic = feed.getPreferences() != null ? feed.getPreferences().getFilter() : null;
            if (basic == null) {
                return items;
            }
            List<FeedItem> kept = new ArrayList<>();
            for (FeedItem item : items) {
                if (basic.shouldAutoDownload(item)) {
                    kept.add(item);
                }
            }
            return kept;
        }
        AdvancedEpisodeFilter advanced = getAdvancedFilterOrBasic(feed);
        if (advanced.isEmpty()) {
            return items;
        }
        List<FeedItem> kept = new ArrayList<>();
        for (Map.Entry<FeedItem, AdvancedEpisodeFilter.Result> entry : evaluate(advanced, items)) {
            if (entry.getValue().matches) {
                kept.add(entry.getKey());
            }
        }
        return kept;
    }

    @NonNull
    public static List<Map.Entry<FeedItem, AdvancedEpisodeFilter.Result>> evaluate(
            @NonNull AdvancedEpisodeFilter filter, @NonNull List<FeedItem> items) {
        Map<Long, String> descriptions = Collections.emptyMap();
        if (filter.needsDescription()) {
            List<Long> ids = new ArrayList<>();
            for (FeedItem item : items) {
                ids.add(item.getId());
            }
            descriptions = queryDescriptions(ids);
        }
        List<Map.Entry<FeedItem, AdvancedEpisodeFilter.Result>> results = new ArrayList<>();
        for (FeedItem item : items) {
            int duration = item.getMedia() != null ? item.getMedia().getDuration() : 0;
            String description = descriptions.get(item.getId());
            if (description == null) {
                description = item.getDescription();
            }
            results.add(new AbstractMap.SimpleImmutableEntry<>(item,
                    filter.evaluate(item.getTitle(), description, duration)));
        }
        return results;
    }

    public static void loadDescriptions(@NonNull List<FeedItem> items) {
        List<Long> ids = new ArrayList<>();
        for (FeedItem item : items) {
            ids.add(item.getId());
        }
        Map<Long, String> descriptions = queryDescriptions(ids);
        for (FeedItem item : items) {
            String description = descriptions.get(item.getId());
            if (description != null) {
                item.setDescriptionIfLonger(description);
            }
        }
    }

    @NonNull
    private static synchronized Map<Long, String> queryDescriptions(@NonNull List<Long> ids) {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try {
            return adapter.getEpisodeFilterDbAdapter().getDescriptions(ids);
        } finally {
            adapter.close();
        }
    }
}
