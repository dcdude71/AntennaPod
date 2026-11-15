package de.danoeh.antennapod.storage.database;

import org.greenrobot.eventbus.EventBus;

import de.danoeh.antennapod.event.FeedAdSkipEvent;

public final class FeedAdSkip {

    private FeedAdSkip() {
    }

    public static synchronized int getSeconds(long feedId) {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try {
            return adapter.getFeedAdSkipDbAdapter().getSeconds(feedId);
        } finally {
            adapter.close();
        }
    }

    public static synchronized void setSeconds(long feedId, int seconds) {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try {
            adapter.getFeedAdSkipDbAdapter().setSeconds(feedId, seconds);
        } finally {
            adapter.close();
        }
        EventBus.getDefault().post(new FeedAdSkipEvent(feedId));
    }
}
