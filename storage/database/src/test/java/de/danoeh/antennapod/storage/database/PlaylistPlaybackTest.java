package de.danoeh.antennapod.storage.database;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.danoeh.antennapod.model.feed.FeedItem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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

    private static List<FeedItem> episodes(long... ids) {
        List<FeedItem> list = new ArrayList<>();
        for (long id : ids) {
            list.add(new FeedItem(id, "Episode " + id, "guid-" + id, null, new Date(id), FeedItem.UNPLAYED, null));
        }
        return list;
    }
}
