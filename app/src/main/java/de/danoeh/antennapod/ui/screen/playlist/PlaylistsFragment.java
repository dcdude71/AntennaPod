package de.danoeh.antennapod.ui.screen.playlist;

import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.ColorUtils;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.actionbutton.ItemActionButton;
import de.danoeh.antennapod.actionbutton.PlayActionButton;
import de.danoeh.antennapod.actionbutton.StreamActionButton;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.event.MessageEvent;
import de.danoeh.antennapod.event.PlaylistEvent;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.playlist.Playlist;
import de.danoeh.antennapod.storage.database.PlaylistPlayback;
import de.danoeh.antennapod.storage.database.PlaylistReader;
import de.danoeh.antennapod.storage.database.PlaylistWriter;
import de.danoeh.antennapod.ui.common.EmptyViewHandler;
import de.danoeh.antennapod.ui.common.ThemeUtils;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class PlaylistsFragment extends Fragment implements Toolbar.OnMenuItemClickListener {
    public static final String TAG = "PlaylistsFragment";
    private static final String KEY_UP_ARROW = "up_arrow";
    private static final int SELECTED_ALPHA = 0x2e;

    private PlaylistAdapter adapter;
    private EmptyViewHandler emptyView;
    private Disposable disposable;
    private Disposable playDisposable;
    private boolean displayUpArrow;
    private View listPane;
    private View paneDivider;
    private View detailPane;
    private boolean wide;
    private long selectedId = PlaylistPanes.NO_PLAYLIST;

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.playlist_panes, container, false);
        listPane = root.findViewById(R.id.listPane);
        paneDivider = root.findViewById(R.id.paneDivider);
        detailPane = root.findViewById(R.id.detailPane);
        MaterialToolbar toolbar = root.findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.playlists_label);
        toolbar.inflateMenu(R.menu.playlists);
        toolbar.setOnMenuItemClickListener(this);
        displayUpArrow = getParentFragmentManager().getBackStackEntryCount() != 0;
        if (savedInstanceState != null) {
            displayUpArrow = savedInstanceState.getBoolean(KEY_UP_ARROW);
        }
        ((MainActivity) getActivity()).setupToolbarToggle(toolbar, displayUpArrow);

        RecyclerView recyclerView = root.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PlaylistAdapter();
        adapter.attachTo(recyclerView, false);

        emptyView = new EmptyViewHandler(getContext());
        emptyView.setIcon(R.drawable.ic_playlist_play);
        emptyView.setTitle(R.string.playlist_empty_title);
        emptyView.setMessage(R.string.playlist_empty_message);
        emptyView.attachToRecyclerView(recyclerView);
        wide = PlaylistPanes.isWide(requireContext());
        selectedId = PlaylistPanes.getSelected(requireContext());
        applyPanes(false);
        return root;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        boolean nowWide = PlaylistPanes.isWide(requireContext());
        if (getView() == null || nowWide == wide) {
            return;
        }
        wide = nowWide;
        applyPanes(true);
        adapter.notifyDataSetChanged();
    }

    private void applyPanes(boolean sizeChanged) {
        PlaylistPanes.layoutPanes(listPane, paneDivider, detailPane, wide);
        if (wide) {
            showDetail(selectedId);
            return;
        }
        Fragment detail = getChildFragmentManager().findFragmentById(R.id.detailPane);
        if (detail != null) {
            getChildFragmentManager().beginTransaction().remove(detail).commit();
        }
        if (sizeChanged && !isHidden() && selectedId != PlaylistPanes.NO_PLAYLIST) {
            ((MainActivity) requireActivity()).loadChildFragment(PlaylistEpisodesFragment.newInstance(selectedId));
        }
    }

    private void showDetail(long playlistId) {
        Fragment detail = getChildFragmentManager().findFragmentById(R.id.detailPane);
        if (playlistId == PlaylistPanes.NO_PLAYLIST) {
            if (detail != null) {
                getChildFragmentManager().beginTransaction().remove(detail).commit();
            }
            return;
        }
        if (detail instanceof PlaylistEpisodesFragment
                && ((PlaylistEpisodesFragment) detail).getPlaylistId() == playlistId) {
            return;
        }
        getChildFragmentManager().beginTransaction()
                .replace(R.id.detailPane, PlaylistEpisodesFragment.newInstance(playlistId))
                .commit();
    }

    private void select(long playlistId) {
        selectedId = playlistId;
        PlaylistPanes.setSelected(requireContext(), playlistId);
    }

    void onSelectedPlaylistDeleted() {
        select(PlaylistPanes.NO_PLAYLIST);
        showDetail(PlaylistPanes.NO_PLAYLIST);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        loadPlaylists();
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        if (disposable != null) {
            disposable.dispose();
        }
        if (playDisposable != null) {
            playDisposable.dispose();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(KEY_UP_ARROW, displayUpArrow);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.playlists_play_from_top) {
            playFromTop();
            return true;
        }
        if (item.getItemId() == R.id.playlist_new) {
            PlaylistNameDialog.show(requireContext(), R.string.playlist_new, "",
                    name -> PlaylistWriter.createPlaylist(new Playlist(name)));
            return true;
        }
        return false;
    }

    private void playFromTop() {
        if (playDisposable != null) {
            playDisposable.dispose();
        }
        playDisposable = Observable.fromCallable(() -> {
            for (Playlist playlist : PlaylistReader.getPlaylists()) {
                List<FeedItem> episodes = PlaylistReader.getEpisodes(playlist.getId());
                if (!episodes.isEmpty() && episodes.get(0).getMedia() != null) {
                    return new Pair<>(playlist.getId(), episodes.get(0));
                }
            }
            return new Pair<Long, FeedItem>(PlaylistPlayback.NO_PLAYLIST, null);
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    FeedItem first = result.second;
                    if (first == null) {
                        EventBus.getDefault().post(new MessageEvent(getString(R.string.playlists_nothing_to_play)));
                        return;
                    }
                    PlaylistPlayback.start(requireContext(), result.first);
                    ItemActionButton button = first.getMedia().isDownloaded()
                            ? new PlayActionButton(first) : new StreamActionButton(first);
                    button.onClick(requireContext());
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlaylistEvent(PlaylistEvent event) {
        loadPlaylists();
    }

    private void loadPlaylists() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Observable.fromCallable(() -> {
            List<Pair<Playlist, Integer>> result = new ArrayList<>();
            for (Playlist playlist : PlaylistReader.getPlaylists()) {
                result.add(new Pair<>(playlist, PlaylistReader.getRules(playlist.getId()).size()));
            }
            return result;
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    adapter.setItems(result);
                    emptyView.updateVisibility();
                    if (wide) {
                        selectExistingPlaylist(result);
                    }
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    private void selectExistingPlaylist(List<Pair<Playlist, Integer>> playlists) {
        long firstId = PlaylistPanes.NO_PLAYLIST;
        for (Pair<Playlist, Integer> item : playlists) {
            if (item.first.getId() == selectedId) {
                showDetail(selectedId);
                return;
            }
            if (firstId == PlaylistPanes.NO_PLAYLIST) {
                firstId = item.first.getId();
            }
        }
        select(firstId);
        showDetail(firstId);
        adapter.notifyDataSetChanged();
    }

    private class PlaylistAdapter extends ReorderableListAdapter<Pair<Playlist, Integer>> {
        @Override
        void bind(@NonNull ViewHolder holder, @NonNull Pair<Playlist, Integer> item) {
            holder.image.setImageResource(R.drawable.ic_playlist_play);
            holder.title.setText(item.first.getName());
            holder.subtitle.setText(getResources().getQuantityString(
                    R.plurals.playlist_num_podcasts, item.second, item.second));
            boolean selected = wide && item.first.getId() == selectedId;
            holder.itemView.setForeground(selected ? new ColorDrawable(ColorUtils.setAlphaComponent(
                    ThemeUtils.getColorFromAttr(requireContext(), R.attr.colorPrimary), SELECTED_ALPHA)) : null);
        }

        @Override
        void onItemClick(@NonNull Pair<Playlist, Integer> item) {
            select(item.first.getId());
            if (wide) {
                showDetail(selectedId);
                notifyDataSetChanged();
            } else {
                ((MainActivity) requireActivity()).loadChildFragment(
                        PlaylistEpisodesFragment.newInstance(item.first.getId()));
            }
        }

        @Override
        void onOrderChanged(@NonNull List<Pair<Playlist, Integer>> newOrder) {
            List<Playlist> playlists = new ArrayList<>();
            for (Pair<Playlist, Integer> item : newOrder) {
                playlists.add(item.first);
            }
            PlaylistWriter.setPlaylistOrder(playlists);
        }
    }
}
