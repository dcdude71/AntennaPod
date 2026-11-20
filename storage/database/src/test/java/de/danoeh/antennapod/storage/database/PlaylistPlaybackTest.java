package de.danoeh.antennapod.storage.database;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.playlist.PlaylistRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PlaylistPlaybackTest {

    @Test
    public void testNextSkipsCurrentEpisode() {
        List<FeedItem> episodes = episodes(1, 2, 3);
        assertEquals(2, PlaylistPlayback.getNextEpisode(episodes, 1, new HashSet<>()).getId());
    }

    @Test
    public void testNextWhenCurrentIsNoLongerInList() {
        List<FeedItem> episodes = episodes(2, 3);
        assertEquals(2, PlaylistPlayback.getNextEpisode(episodes, 1, new HashSet<>()).getId());
    }

    @Test
    public void testNextIgnoresSkippedEpisodes() {
        List<FeedItem> episodes = episodes(1, 2, 3);
        Set<Long> skipped = new HashSet<>();
        skipped.add(1L);
        assertEquals(3, PlaylistPlayback.getNextEpisode(episodes, 2, skipped).getId());
    }

    @Test
    public void testNoNextAtEndOfPlaylist() {
        assertNull(PlaylistPlayback.getNextEpisode(episodes(1), 1, new HashSet<>()));
        assertNull(PlaylistPlayback.getNextEpisode(new ArrayList<>(), 1, new HashSet<>()));
    }

    @Test
    public void testFollowingPlaylistsComeAfterCurrent() {
        assertEquals(Arrays.asList(30L, 40L),
                PlaylistPlayback.getFollowingPlaylistIds(Arrays.asList(10L, 20L, 30L, 40L), 20L));
    }

    @Test
    public void testNoFollowingPlaylistAfterLast() {
        assertTrue(PlaylistPlayback.getFollowingPlaylistIds(Arrays.asList(10L, 20L), 20L).isEmpty());
    }

    @Test
    public void testNoFollowingPlaylistWhenCurrentIsDeleted() {
        assertTrue(PlaylistPlayback.getFollowingPlaylistIds(Arrays.asList(10L, 20L), 99L).isEmpty());
    }

    @Test
    public void testManualNextFollowsListOrderFromCurrent() {
        List<FeedItem> episodes = episodes(1, 2, 3, 4);
        assertEquals(4, PlaylistPlayback.getNextManualEpisode(episodes, 3, new HashSet<>(), false).getId());
        assertNull(PlaylistPlayback.getNextManualEpisode(episodes, 4, new HashSet<>(), false));
    }

    @Test
    public void testManualNextSkipsPlayedUnlessKept() {
        List<FeedItem> episodes = episodes(1, 2, 3);
        episodes.get(1).setPlayed(true);
        assertEquals(3, PlaylistPlayback.getNextManualEpisode(episodes, 1, new HashSet<>(), false).getId());
        assertEquals(2, PlaylistPlayback.getNextManualEpisode(episodes, 1, new HashSet<>(), true).getId());
    }

    @Test
    public void testManualNextStartsAtTopWhenCurrentIsNotInList() {
        Set<Long> skipped = new HashSet<>();
        skipped.add(2L);
        assertEquals(3, PlaylistPlayback.getNextManualEpisode(episodes(2, 3), 9, skipped, false).getId());
    }

    @Test
    public void testAutoDownloadLimitsAllAndLargeCounts() {
        List<PlaylistRule> rules = Arrays.asList(rule(PlaylistRule.ALL_EPISODES), rule(25), rule(4));
        List<PlaylistRule> limited = PlaylistReader.limitForAutoDownload(rules);
        assertEquals(PlaylistRule.MAX_AUTO_DOWNLOAD_EPISODES, limited.get(0).getEpisodeCount());
        assertEquals(PlaylistRule.MAX_AUTO_DOWNLOAD_EPISODES, limited.get(1).getEpisodeCount());
        assertEquals(4, limited.get(2).getEpisodeCount());
        assertEquals(PlaylistRule.ALL_EPISODES, rules.get(0).getEpisodeCount());
    }

    private static PlaylistRule rule(int count) {
        return new PlaylistRule(1, 1, 1, count, PlaylistRule.PickFrom.OLDEST,
                PlaylistRule.PlayOrder.OLDEST_FIRST, PlaylistRule.MIN_PRIORITY, 0);
    }

    private static List<FeedItem> episodes(long... ids) {
        List<FeedItem> list = new ArrayList<>();
        for (long id : ids) {
            list.add(new FeedItem(id, "Episode " + id, "guid-" + id, null, new Date(id), FeedItem.UNPLAYED, null));
        }
        return list;
    }
}
