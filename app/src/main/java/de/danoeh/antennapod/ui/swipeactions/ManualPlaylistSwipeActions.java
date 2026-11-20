package de.danoeh.antennapod.ui.swipeactions;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.ui.episodeslist.EpisodeItemViewHolder;

public class ManualPlaylistSwipeActions extends SwipeActions {

    public interface Listener {
        boolean onMove(int from, int to);

        void onMoveFinished();

        void onRemove(@NonNull FeedItem item);
    }

    private final Listener listener;
    private boolean moved = false;

    public ManualPlaylistSwipeActions(Fragment fragment, String tag, Listener listener) {
        super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, fragment, tag);
        this.listener = listener;
    }

    @Override
    public void reloadPreference() {
        actions = new Actions("");
        RemoveFromPlaylistSwipeAction remove = new RemoveFromPlaylistSwipeAction();
        actions.right = remove;
        actions.left = remove;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        int from = viewHolder.getBindingAdapterPosition();
        int to = target.getBindingAdapterPosition();
        if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) {
            return false;
        }
        boolean result = listener.onMove(from, to);
        moved |= result;
        return result;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int swipeDir) {
        FeedItem item = ((EpisodeItemViewHolder) viewHolder).getFeedItem();
        if (item != null) {
            listener.onRemove(item);
        }
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        if (moved) {
            moved = false;
            listener.onMoveFinished();
        }
    }
}
