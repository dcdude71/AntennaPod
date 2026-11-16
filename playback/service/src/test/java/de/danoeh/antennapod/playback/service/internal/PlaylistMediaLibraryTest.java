package de.danoeh.antennapod.playback.service.internal;

import android.content.Context;

import androidx.media3.common.MediaItem;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

import de.danoeh.antennapod.model.playlist.Playlist;
import de.danoeh.antennapod.storage.database.PlaylistPlayback;
import de.danoeh.antennapod.storage.database.PlaylistWriter;
import de.danoeh.antennapod.storage.database.PodDBAdapter;
import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class PlaylistMediaLibraryTest {
    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        UserPreferences.init(context);
        PlaybackPreferences.init(context);
        PodDBAdapter.init(context);
        PodDBAdapter.deleteDatabase();
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.close();
    }

    @After
    public void tearDown() {
        PodDBAdapter.tearDownTests();
    }

    @Test
    public void testBrowsableIds() {
        assertTrue(PlaylistMediaLibrary.isBrowsableId(PlaylistMediaLibrary.MEDIA_ID_PLAYLISTS));
        assertTrue(PlaylistMediaLibrary.isBrowsableId("Playlist:3"));
        assertFalse(PlaylistMediaLibrary.isBrowsableId("PlaylistEpisode:3:12"));
        assertFalse(PlaylistMediaLibrary.isBrowsableId("12"));
    }

    @Test
    public void testEpisodeMediaIdRoundTrip() {
        String id = PlaylistMediaLibrary.toEpisodeMediaId(3, "12");
        assertEquals("PlaylistEpisode:3:12", id);
        assertEquals(3, PlaylistMediaLibrary.parsePlaylistId("Playlist:3"));
        assertEquals("12", PlaylistMediaLibrary.resolveMediaId(context, "12"));
    }

    @Test
    public void testMalformedEpisodeIdDoesNotStartPlaylist() {
        String[] invalidIds = {"PlaylistEpisode:5", "PlaylistEpisode:5:abc", "PlaylistEpisode:x:12"};
        for (String id : invalidIds) {
            try {
                PlaylistMediaLibrary.resolveMediaId(context, id);
            } catch (NumberFormatException expected) {
                continue;
            }
            throw new AssertionError("Accepted " + id);
        }
        assertEquals(PlaylistPlayback.NO_PLAYLIST, PlaylistPlayback.getActivePlaylist(context));
        assertEquals("12", PlaylistMediaLibrary.resolveMediaId(context, "PlaylistEpisode:5:12"));
        assertEquals(5, PlaylistPlayback.getActivePlaylist(context));
    }

    @Test
    public void testOnlyVisiblePlaylistsAreListed() throws Exception {
        assertNull(PlaylistMediaLibrary.createRootItem(context));

        Playlist shown = new Playlist("Commute");
        long shownId = PlaylistWriter.createPlaylist(shown).get();
        Playlist hidden = new Playlist("Home");
        hidden.setShowInAndroidAuto(false);
        PlaylistWriter.createPlaylist(hidden).get();

        assertNotNull(PlaylistMediaLibrary.createRootItem(context));
        List<MediaItem> children = PlaylistMediaLibrary.getChildren(context,
                PlaylistMediaLibrary.MEDIA_ID_PLAYLISTS);
        assertEquals(1, children.size());
        assertEquals("Playlist:" + shownId, children.get(0).mediaId);
        assertEquals("Commute", String.valueOf(children.get(0).mediaMetadata.title));
        assertTrue(children.get(0).mediaMetadata.isBrowsable);
    }
}
