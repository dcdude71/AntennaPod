package de.danoeh.antennapod.storage.database;

import android.content.Context;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class FeedAdSkipTest {

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.getApplication();
        UserPreferences.init(context);
        PodDBAdapter.init(context);
        PodDBAdapter.deleteDatabase();
    }

    @After
    public void tearDown() {
        PodDBAdapter.tearDownTests();
    }

    @Test
    public void testSetAndGet() {
        assertEquals(0, FeedAdSkip.getSeconds(1));
        FeedAdSkip.setSeconds(1, 30);
        FeedAdSkip.setSeconds(2, 45);
        assertEquals(30, FeedAdSkip.getSeconds(1));
        assertEquals(45, FeedAdSkip.getSeconds(2));
        FeedAdSkip.setSeconds(1, 20);
        assertEquals(20, FeedAdSkip.getSeconds(1));
        FeedAdSkip.setSeconds(1, 0);
        assertEquals(0, FeedAdSkip.getSeconds(1));
    }

    @Test
    public void testRemovingFeedRemovesAdSkip() {
        Feed feed = new Feed("url", null, "Feed");
        feed.setItems(new ArrayList<>());
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();
        FeedAdSkip.setSeconds(feed.getId(), 30);

        adapter.open();
        adapter.removeFeed(feed);
        adapter.close();
        assertEquals(0, FeedAdSkip.getSeconds(feed.getId()));
    }
}
