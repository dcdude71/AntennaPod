package de.danoeh.antennapod.model.playlist;

public class PlaylistRule {
    public static final int ALL_EPISODES = 0;
    public static final int MIN_PRIORITY = 1;
    public static final int MAX_PRIORITY = 10;
    public static final int MAX_AUTO_DOWNLOAD_EPISODES = 10;

    public enum PickFrom {
        MOST_RECENT(0),
        OLDEST(1);

        public final int code;

        PickFrom(int code) {
            this.code = code;
        }

        public static PickFrom fromCode(int code) {
            for (PickFrom pickFrom : values()) {
                if (code == pickFrom.code) {
                    return pickFrom;
                }
            }
            return MOST_RECENT;
        }
    }

    public enum PlayOrder {
        OLDEST_FIRST(0),
        NEWEST_FIRST(1);

        public final int code;

        PlayOrder(int code) {
            this.code = code;
        }

        public static PlayOrder fromCode(int code) {
            for (PlayOrder playOrder : values()) {
                if (code == playOrder.code) {
                    return playOrder;
                }
            }
            return OLDEST_FIRST;
        }
    }

    private long id;
    private long playlistId;
    private long feedId;
    private int episodeCount;
    private PickFrom pickFrom;
    private PlayOrder playOrder;
    private int priority;
    private int position;

    public PlaylistRule(long id, long playlistId, long feedId, int episodeCount, PickFrom pickFrom,
                        PlayOrder playOrder, int priority, int position) {
        this.id = id;
        this.playlistId = playlistId;
        this.feedId = feedId;
        this.episodeCount = episodeCount;
        this.pickFrom = pickFrom;
        this.playOrder = playOrder;
        this.priority = priority;
        this.position = position;
    }

    public PlaylistRule(long playlistId, long feedId) {
        this(0, playlistId, feedId, ALL_EPISODES, PickFrom.MOST_RECENT, PlayOrder.OLDEST_FIRST, MIN_PRIORITY, 0);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getPlaylistId() {
        return playlistId;
    }

    public long getFeedId() {
        return feedId;
    }

    public void setFeedId(long feedId) {
        this.feedId = feedId;
    }

    public int getEpisodeCount() {
        return episodeCount;
    }

    public void setEpisodeCount(int episodeCount) {
        this.episodeCount = Math.max(ALL_EPISODES, episodeCount);
    }

    public PickFrom getPickFrom() {
        return pickFrom;
    }

    public void setPickFrom(PickFrom pickFrom) {
        this.pickFrom = pickFrom;
    }

    public PlayOrder getPlayOrder() {
        return playOrder;
    }

    public void setPlayOrder(PlayOrder playOrder) {
        this.playOrder = playOrder;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = Math.max(MIN_PRIORITY, Math.min(MAX_PRIORITY, priority));
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
