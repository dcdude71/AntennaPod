package de.danoeh.antennapod.storage.database;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.playlist.PlaylistRule;

public final class PlaylistEngine {

    public interface EpisodeSource {
        @NonNull
        List<FeedItem> getNewest(long feedId, int limit);

        @NonNull
        List<FeedItem> getOldestUnplayed(long feedId, int limit);
    }

    private PlaylistEngine() {
    }

    @NonNull
    public static List<FeedItem> computeEpisodes(@NonNull List<PlaylistRule> rules, @NonNull EpisodeSource source) {
        List<PlaylistRule> sortedRules = new ArrayList<>(rules);
        Collections.sort(sortedRules, (a, b) -> {
            if (a.getPriority() != b.getPriority()) {
                return Integer.compare(a.getPriority(), b.getPriority());
            }
            if (a.getPosition() != b.getPosition()) {
                return Integer.compare(a.getPosition(), b.getPosition());
            }
            return Long.compare(a.getId(), b.getId());
        });

        List<FeedItem> result = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();
        for (PlaylistRule rule : sortedRules) {
            for (FeedItem item : pick(rule, source)) {
                if (seenIds.add(item.getId())) {
                    result.add(item);
                }
            }
        }
        return result;
    }

    @NonNull
    static List<FeedItem> pick(@NonNull PlaylistRule rule, @NonNull EpisodeSource source) {
        List<FeedItem> picks = new ArrayList<>();
        if (rule.getPickFrom() == PlaylistRule.PickFrom.MOST_RECENT
                && rule.getEpisodeCount() != PlaylistRule.ALL_EPISODES) {
            for (FeedItem item : source.getNewest(rule.getFeedId(), rule.getEpisodeCount())) {
                if (!item.isPlayed()) {
                    picks.add(item);
                }
            }
        } else {
            int limit = rule.getEpisodeCount() == PlaylistRule.ALL_EPISODES
                    ? Integer.MAX_VALUE : rule.getEpisodeCount();
            picks.addAll(source.getOldestUnplayed(rule.getFeedId(), limit));
        }

        final boolean newestFirst = rule.getPlayOrder() == PlaylistRule.PlayOrder.NEWEST_FIRST;
        Collections.sort(picks, (a, b) -> {
            int result = Long.compare(pubDateMillis(a), pubDateMillis(b));
            return newestFirst ? -result : result;
        });
        return picks;
    }

    private static long pubDateMillis(FeedItem item) {
        return item.getPubDate() != null ? item.getPubDate().getTime() : 0;
    }
}
