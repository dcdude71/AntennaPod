package de.danoeh.antennapod.ui.swipeactions;

import android.content.Context;

import androidx.fragment.app.Fragment;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;

public class RemoveFromPlaylistSwipeAction implements SwipeAction {
    public static final String REMOVE_FROM_PLAYLIST = "REMOVE_FROM_PLAYLIST";

    @Override
    public String getId() {
        return REMOVE_FROM_PLAYLIST;
    }

    @Override
    public String getTitle(Context context) {
        return context.getString(R.string.remove_from_playlist_label);
    }

    @Override
    public int getActionIcon() {
        return R.drawable.ic_playlist_remove;
    }

    @Override
    public int getActionColor() {
        return R.attr.colorAccent;
    }

    @Override
    public void performAction(FeedItem item, Fragment fragment, FeedItemFilter filter) {
    }

    @Override
    public boolean willRemove(FeedItemFilter filter, FeedItem item) {
        return true;
    }
}
