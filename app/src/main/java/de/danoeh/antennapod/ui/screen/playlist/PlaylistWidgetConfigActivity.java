package de.danoeh.antennapod.ui.screen.playlist;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.playlist.Playlist;
import de.danoeh.antennapod.storage.database.PlaylistReader;
import de.danoeh.antennapod.ui.common.ThemeSwitcher;
import de.danoeh.antennapod.ui.widget.PlaylistWidget;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class PlaylistWidgetConfigActivity extends AppCompatActivity {
    private static final String TAG = "PlaylistWidgetConfig";

    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private final List<Playlist> playlists = new ArrayList<>();
    private final Set<Long> selected = new HashSet<>();
    private Adapter adapter;
    private ItemTouchHelper touchHelper;
    private TextView countView;
    private MaterialButton saveButton;
    private Disposable disposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeSwitcher.getNoTitleTheme(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playlist_widget_config);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        Intent result = new Intent();
        result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_CANCELED, result);
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        TextView hint = findViewById(R.id.hint);
        hint.setText(getString(R.string.playlist_widget_settings_hint, PlaylistWidget.MAX_PLAYLISTS));
        countView = findViewById(R.id.selectedCount);
        saveButton = findViewById(R.id.saveButton);
        boolean configured = PlaylistWidget.isConfigured(this, appWidgetId);
        saveButton.setText(configured ? R.string.playlist_widget_save : R.string.playlist_widget_add);
        saveButton.setOnClickListener(v -> save());

        RecyclerView list = findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new Adapter();
        list.setAdapter(adapter);
        touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int from = viewHolder.getBindingAdapterPosition();
                int to = target.getBindingAdapterPosition();
                if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) {
                    return false;
                }
                Collections.swap(playlists, from, to);
                adapter.notifyItemMoved(from, to);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }

            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                adapter.notifyDataSetChanged();
            }
        });
        touchHelper.attachToRecyclerView(list);

        disposable = Single.fromCallable(() -> {
            List<Playlist> all = PlaylistReader.getPlaylists();
            List<Long> ids = PlaylistWidget.getPlaylistIds(this, appWidgetId);
            if (ids.isEmpty() && !configured) {
                for (int i = 0; i < Math.min(PlaylistWidget.MAX_PLAYLISTS, all.size()); i++) {
                    ids.add(all.get(i).getId());
                }
            }
            all.add(0, PlaylistWidget.allPlaylists(this));
            List<Playlist> ordered = new ArrayList<>();
            for (long id : ids) {
                for (Playlist playlist : all) {
                    if (playlist.getId() == id && ordered.size() < PlaylistWidget.MAX_PLAYLISTS) {
                        ordered.add(playlist);
                    }
                }
            }
            for (Playlist playlist : all) {
                if (!ordered.contains(playlist)) {
                    ordered.add(playlist);
                }
            }
            return new Object[] {ordered, ids};
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(loaded -> {
                    @SuppressWarnings("unchecked") List<Playlist> ordered = (List<Playlist>) loaded[0];
                    @SuppressWarnings("unchecked") List<Long> ids = (List<Long>) loaded[1];
                    playlists.clear();
                    playlists.addAll(ordered);
                    selected.clear();
                    for (Playlist playlist : ordered) {
                        if (ids.contains(playlist.getId()) && selected.size() < PlaylistWidget.MAX_PLAYLISTS) {
                            selected.add(playlist.getId());
                        }
                    }
                    refresh();
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (disposable != null) {
            disposable.dispose();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void refresh() {
        adapter.notifyDataSetChanged();
        countView.setText(getString(R.string.playlist_widget_selected, selected.size(), PlaylistWidget.MAX_PLAYLISTS));
    }

    private void toggle(@NonNull Playlist playlist) {
        if (selected.contains(playlist.getId())) {
            selected.remove(playlist.getId());
        } else if (selected.size() < PlaylistWidget.MAX_PLAYLISTS) {
            selected.add(playlist.getId());
        }
        refresh();
    }

    private int getPosition(@NonNull Playlist playlist) {
        int position = 0;
        for (Playlist item : playlists) {
            if (selected.contains(item.getId())) {
                position++;
            }
            if (item == playlist) {
                return position;
            }
        }
        return position;
    }

    private void save() {
        List<Long> ids = new ArrayList<>();
        for (Playlist playlist : playlists) {
            if (selected.contains(playlist.getId())) {
                ids.add(playlist.getId());
            }
        }
        PlaylistWidget.setPlaylistIds(this, appWidgetId, ids);
        Intent result = new Intent();
        result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, result);
        finish();
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.playlist_widget_config_item, parent, false));
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Playlist playlist = playlists.get(position);
            boolean checked = selected.contains(playlist.getId());
            final boolean enabled = checked || selected.size() < PlaylistWidget.MAX_PLAYLISTS;
            holder.title.setText(playlist.getName());
            holder.subtitle.setText(checked
                    ? getString(R.string.playlist_widget_position, getPosition(playlist))
                    : getString(R.string.playlist_widget_not_on_widget));
            holder.check.setChecked(checked);
            holder.check.setEnabled(enabled);
            holder.itemView.setEnabled(enabled);
            holder.itemView.setAlpha(enabled ? 1f : 0.5f);
            holder.itemView.setOnClickListener(v -> toggle(playlist));
            holder.dragHandle.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    touchHelper.startDrag(holder);
                }
                return false;
            });
        }

        @Override
        public int getItemCount() {
            return playlists.size();
        }
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
        final CheckBox check;
        final TextView title;
        final TextView subtitle;
        final View dragHandle;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            check = itemView.findViewById(R.id.check);
            title = itemView.findViewById(R.id.title);
            subtitle = itemView.findViewById(R.id.subtitle);
            dragHandle = itemView.findViewById(R.id.dragHandle);
        }
    }
}
