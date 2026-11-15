package de.danoeh.antennapod.ui.screen.playlist;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.danoeh.antennapod.R;

abstract class ReorderableListAdapter<T> extends RecyclerView.Adapter<ReorderableListAdapter.ViewHolder> {
    private final List<T> items = new ArrayList<>();
    private ItemTouchHelper touchHelper;

    void setItems(List<T> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    List<T> getItems() {
        return new ArrayList<>(items);
    }

    void attachTo(RecyclerView recyclerView, boolean swipeToRemove) {
        int swipeDirs = swipeToRemove ? ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT : 0;
        touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, swipeDirs) {
            private boolean moved = false;

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int from = viewHolder.getBindingAdapterPosition();
                int to = target.getBindingAdapterPosition();
                if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) {
                    return false;
                }
                Collections.swap(items, from, to);
                notifyItemMoved(from, to);
                moved = true;
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION) {
                    return;
                }
                T item = items.remove(position);
                notifyItemRemoved(position);
                onItemSwiped(item);
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                if (moved) {
                    moved = false;
                    onOrderChanged(getItems());
                }
            }
        });
        touchHelper.attachToRecyclerView(recyclerView);
        recyclerView.setAdapter(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.playlist_list_item, parent, false));
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        T item = items.get(position);
        bind(holder, item);
        holder.itemView.setOnClickListener(v -> onItemClick(item));
        holder.dragHandle.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN && touchHelper != null) {
                touchHelper.startDrag(holder);
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    abstract void bind(@NonNull ViewHolder holder, @NonNull T item);

    abstract void onItemClick(@NonNull T item);

    abstract void onOrderChanged(@NonNull List<T> newOrder);

    void onItemSwiped(@NonNull T item) {
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView title;
        final TextView subtitle;
        final View dragHandle;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image);
            title = itemView.findViewById(R.id.title);
            subtitle = itemView.findViewById(R.id.subtitle);
            dragHandle = itemView.findViewById(R.id.dragHandle);
        }
    }
}
