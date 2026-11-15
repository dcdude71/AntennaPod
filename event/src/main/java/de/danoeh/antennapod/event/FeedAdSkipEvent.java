package de.danoeh.antennapod.event;

public class FeedAdSkipEvent {
    public final long feedId;

    public FeedAdSkipEvent(long feedId) {
        this.feedId = feedId;
    }
}
