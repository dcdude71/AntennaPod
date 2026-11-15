package de.danoeh.antennapod.storage.database;

import androidx.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.playlist.PlaylistRule;

import static org.junit.Assert.assertEquals;

public class PlaylistEngineTest {
    private static final long FEED_A = 1;
    private static final long FEED_B = 2;
    private static final long FEED_C = 3;

    private FakeSource source;
    private long nextRuleId;

    @Before
    public void setUp() {
        source = new FakeSource();
        nextRuleId = 1;
    }

    @Test
    public void testPriorityOrdersRules() {
        source.addEpisodes(FEED_A, 100, 1);
        source.addEpisodes(FEED_B, 200, 1);
        source.addEpisodes(FEED_C, 300, 1);
        List<PlaylistRule> rules = new ArrayList<>();
        rules.add(rule(FEED_A, 0, PlaylistRule.PickFrom.MOST_RECENT, PlaylistRule.PlayOrder.OLDEST_FIRST, 3, 0));
        rules.add(rule(FEED_B, 0, PlaylistRule.PickFrom.MOST_RECENT, PlaylistRule.PlayOrder.OLDEST_FIRST, 1, 1));
        rules.add(rule(FEED_C, 0, PlaylistRule.PickFrom.MOST_RECENT, PlaylistRule.PlayOrder.OLDEST_FIRST, 2, 2));

        assertEquals(List.of(200L, 300L, 100L), ids(PlaylistEngine.computeEpisodes(rules, source)));
    }

    @Test
    public void testSamePriorityUsesRuleOrder() {
        source.addEpisodes(FEED_A, 100, 2);
        source.addEpisodes(FEED_B, 200, 2);
        List<PlaylistRule> rules = new ArrayList<>();
        rules.add(rule(FEED_A, 0, PlaylistRule.PickFrom.MOST_RECENT, PlaylistRule.PlayOrder.OLDEST_FIRST, 1, 1));
        rules.add(rule(FEED_B, 0, PlaylistRule.PickFrom.MOST_RECENT, PlaylistRule.PlayOrder.OLDEST_FIRST, 1, 0));

        assertEquals(List.of(200L, 201L, 100L, 101L), ids(PlaylistEngine.computeEpisodes(rules, source)));
    }

    @Test
    public void testMostRecentPlaysOldestFirst() {
        source.addEpisodes(FEED_A, 100, 10);
        List<PlaylistRule> rules = Collections.singletonList(
                rule(FEED_A, 4, PlaylistRule.PickFrom.MOST_RECENT, PlaylistRule.PlayOrder.OLDEST_FIRST, 1, 0));

        assertEquals(List.of(106L, 107L, 108L, 109L), ids(PlaylistEngine.computeEpisodes(rules, source)));
    }

    @Test
    public void testMostRecentPlaysNewestFirst() {
        source.addEpisodes(FEED_A, 100, 10);
        List<PlaylistRule> rules = Collections.singletonList(
                rule(FEED_A, 4, PlaylistRule.PickFrom.MOST_RECENT, PlaylistRule.PlayOrder.NEWEST_FIRST, 1, 0));

        assertEquals(List.of(109L, 108L, 107L, 106L), ids(PlaylistEngine.computeEpisodes(rules, source)));
    }

    @Test
    public void testMostRecentDoesNotWalkIntoBackCatalog() {
        source.addEpisodes(FEED_A, 100, 10);
        source.setPlayed(106);
        List<PlaylistRule> rules = Collections.singletonList(
                rule(FEED_A, 4, PlaylistRule.PickFrom.MOST_RECENT, PlaylistRule.PlayOrder.OLDEST_FIRST, 1, 0));

        assertEquals(List.of(107L, 108L, 109L), ids(PlaylistEngine.computeEpisodes(rules, source)));

        source.setPlayed(107);
        source.setPlayed(108);
        source.setPlayed(109);
        assertEquals(List.of(), ids(PlaylistEngine.computeEpisodes(rules, source)));
    }

    @Test
    public void testOldestTakesNextUnplayed() {
        source.addEpisodes(FEED_A, 100, 10);
        source.setPlayed(100);
        source.setPlayed(101);
        List<PlaylistRule> rules = Collections.singletonList(
                rule(FEED_A, 3, PlaylistRule.PickFrom.OLDEST, PlaylistRule.PlayOrder.OLDEST_FIRST, 1, 0));

        assertEquals(List.of(102L, 103L, 104L), ids(PlaylistEngine.computeEpisodes(rules, source)));

        source.setPlayed(102);
        assertEquals(List.of(103L, 104L, 105L), ids(PlaylistEngine.computeEpisodes(rules, source)));
    }

    @Test
    public void testOldestPlaysNewestFirst() {
        source.addEpisodes(FEED_A, 100, 10);
        List<PlaylistRule> rules = Collections.singletonList(
                rule(FEED_A, 3, PlaylistRule.PickFrom.OLDEST, PlaylistRule.PlayOrder.NEWEST_FIRST, 1, 0));

        assertEquals(List.of(102L, 101L, 100L), ids(PlaylistEngine.computeEpisodes(rules, source)));
    }

    @Test
    public void testAllEpisodesIncludesEveryUnplayedEpisode() {
        source.addEpisodes(FEED_A, 100, 5);
        source.setPlayed(102);
        List<PlaylistRule> rules = Collections.singletonList(rule(FEED_A, PlaylistRule.ALL_EPISODES,
                PlaylistRule.PickFrom.MOST_RECENT, PlaylistRule.PlayOrder.OLDEST_FIRST, 1, 0));

        assertEquals(List.of(100L, 101L, 103L, 104L), ids(PlaylistEngine.computeEpisodes(rules, source)));
    }

    @Test
    public void testSameEpisodeIsOnlyIncludedOnce() {
        source.addEpisodes(FEED_A, 100, 5);
        List<PlaylistRule> rules = new ArrayList<>();
        rules.add(rule(FEED_A, 2, PlaylistRule.PickFrom.MOST_RECENT, PlaylistRule.PlayOrder.NEWEST_FIRST, 1, 0));
        rules.add(rule(FEED_A, 3, PlaylistRule.PickFrom.MOST_RECENT, PlaylistRule.PlayOrder.NEWEST_FIRST, 2, 1));

        assertEquals(List.of(104L, 103L, 102L), ids(PlaylistEngine.computeEpisodes(rules, source)));
    }

    @Test
    public void testNoRules() {
        assertEquals(List.of(), ids(PlaylistEngine.computeEpisodes(new ArrayList<>(), source)));
    }

    private PlaylistRule rule(long feedId, int count, PlaylistRule.PickFrom pickFrom,
                              PlaylistRule.PlayOrder playOrder, int priority, int position) {
        return new PlaylistRule(nextRuleId++, 1, feedId, count, pickFrom, playOrder, priority, position);
    }

    private static List<Long> ids(List<FeedItem> items) {
        List<Long> ids = new ArrayList<>();
        for (FeedItem item : items) {
            ids.add(item.getId());
        }
        return ids;
    }

    private static class FakeSource implements PlaylistEngine.EpisodeSource {
        private final Map<Long, List<FeedItem>> episodes = new HashMap<>();
        private final Map<Long, FeedItem> byId = new HashMap<>();

        void addEpisodes(long feedId, long firstId, int count) {
            List<FeedItem> list = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                long id = firstId + i;
                FeedItem item = new FeedItem(id, "Episode " + id, "guid-" + id, null,
                        new Date(1_000_000L * id), FeedItem.UNPLAYED, null);
                list.add(item);
                byId.put(id, item);
            }
            episodes.put(feedId, list);
        }

        void setPlayed(long id) {
            byId.get(id).setPlayed(true);
        }

        @NonNull
        @Override
        public List<FeedItem> getNewest(long feedId, int limit) {
            List<FeedItem> list = new ArrayList<>(episodes.getOrDefault(feedId, new ArrayList<>()));
            Collections.reverse(list);
            return list.subList(0, Math.min(limit, list.size()));
        }

        @NonNull
        @Override
        public List<FeedItem> getOldestUnplayed(long feedId, int limit) {
            List<FeedItem> list = new ArrayList<>();
            for (FeedItem item : episodes.getOrDefault(feedId, new ArrayList<>())) {
                if (!item.isPlayed() && list.size() < limit) {
                    list.add(item);
                }
            }
            return list;
        }
    }
}
