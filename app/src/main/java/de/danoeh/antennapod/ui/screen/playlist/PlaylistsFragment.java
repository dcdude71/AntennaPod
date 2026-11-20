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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.actionbutton.ItemActionButton;
import de.danoeh.antennapod.actionbutton.PlayActionButton;
import de.danoeh.antennapod.actionbutton.StreamActionButton;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.event.FeedItemEvent;
import de.danoeh.antennapod.event.MessageEvent;
import de.danoeh.antennapod.event.PlaylistEvent;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.playlist.Playlist;
import de.danoeh.antennapod.model.playlist.PlaylistRule;
import de.danoeh.antennapod.storage.database.PlaylistPlayback;
import de.danoeh.antennapod.storage.database.PlaylistReader;
import de.danoeh.antennapod.storage.database.PlaylistWriter;
import de.danoeh.antennapod.ui.common.EmptyViewHandler;
import de.danoeh.antennapod.ui.common.PlaylistTime;
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
    private Set<Long> feedIds = new HashSet<>();

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
        if (item.getItemId() == R.id.playlists_add_to_home_screen) {
            if (!PlaylistShortcuts.pinAllPlaylists(requireContext())) {
                EventBus.getDefault().post(new MessageEvent(getString(R.string.playlist_shortcut_unsupported)));
            }
            return true;
        }
        if (item.getItemId() == R.id.playlist_new) {
            PlaylistKindDialog.show(requireContext(), (name, manual) -> {
                Playlist playlist = new Playlist(name);
                playlist.setManual(manual);
                PlaylistWriter.createPlaylist(playlist);
            });
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFeedItemChanged(FeedItemEvent event) {
        for (FeedItem item : event.items) {
            if (item != null && feedIds.contains(item.getFeedId())) {
                loadPlaylists();
                return;
            }
        }
    }

    private void loadPlaylists() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Observable.fromCallable(() -> {
            List<PlaylistSummary> result = new ArrayList<>();
            Set<Long> ruleFeedIds = new HashSet<>();
            for (Playlist playlist : PlaylistReader.getPlaylists()) {
                List<PlaylistRule> rules = PlaylistReader.getRules(playlist.getId());
                for (PlaylistRule rule : rules) {
                    ruleFeedIds.add(rule.getFeedId());
                }
                List<FeedItem> episodes = PlaylistReader.getEpisodes(playlist.getId());
                if (playlist.isManual()) {
                    for (FeedItem episode : episodes) {
                        ruleFeedIds.add(episode.getFeedId());
                    }
                }
                result.add(new PlaylistSummary(playlist, rules.size(), episodes.size(),
                        PlaylistReader.getTimeLeftMillis(episodes)));
            }
            return new Pair<>(result, ruleFeedIds);
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(loaded -> {
                    List<PlaylistSummary> result = loaded.first;
                    feedIds = loaded.second;
                    adapter.setItems(result);
                    emptyView.updateVisibility();
                    if (wide) {
                        selectExistingPlaylist(result);
                    }
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    private void selectExistingPlaylist(List<PlaylistSummary> playlists) {
        long firstId = PlaylistPanes.NO_PLAYLIST;
        for (PlaylistSummary item : playlists) {
            if (item.playlist.getId() == selectedId) {
                showDetail(selectedId);
                return;
            }
            if (firstId == PlaylistPanes.NO_PLAYLIST) {
                firstId = item.playlist.getId();
            }
        }
        select(firstId);
        showDetail(firstId);
        adapter.notifyDataSetChanged();
    }

    private static class PlaylistSummary {
        final Playlist playlist;
        final int podcasts;
        final int episodes;
        final long timeLeftMillis;

        PlaylistSummary(Playlist playlist, int podcasts, int episodes, long timeLeftMillis) {
            this.playlist = playlist;
            this.podcasts = podcasts;
            this.episodes = episodes;
            this.timeLeftMillis = timeLeftMillis;
        }
    }

    private class PlaylistAdapter extends ReorderableListAdapter<PlaylistSummary> {
        @Override
        void bind(@NonNull ViewHolder holder, @NonNull PlaylistSummary item) {
            PlaylistKindUi.bindTile(holder.image, item.playlist.isManual());
            holder.title.setText(item.playlist.getName());
            String summary = getResources().getQuantityString(R.plurals.playlist_num_episodes,
                    item.episodes, item.episodes);
            if (!item.playlist.isManual()) {
                summary = getString(R.string.playlist_summary, getResources().getQuantityString(
                        R.plurals.playlist_num_podcasts, item.podcasts, item.podcasts), summary);
            }
            if (item.timeLeftMillis > 0) {
                summary = getString(R.string.playlist_summary, summary, getString(R.string.playlist_time_left,
                        PlaylistTime.format(requireContext(), item.timeLeftMillis)));
            }
            holder.subtitle.setText(getString(R.string.playlist_summary, getString(item.playlist.isManual()
                    ? R.string.playlist_kind_manual : R.string.playlist_kind_smart), summary));
            boolean selected = wide && item.playlist.getId() == selectedId;
            holder.itemView.setForeground(selected ? new ColorDrawable(ColorUtils.setAlphaComponent(
                    ThemeUtils.getColorFromAttr(requireContext(), R.attr.colorPrimary), SELECTED_ALPHA)) : null);
        }

        @Override
        void onItemClick(@NonNull PlaylistSummary item) {
            select(item.playlist.getId());
            if (wide) {
                showDetail(selectedId);
                notifyDataSetChanged();
            } else {
                ((MainActivity) requireActivity()).loadChildFragment(
                        PlaylistEpisodesFragment.newInstance(item.playlist.getId()));
            }
        }

        @Override
        void onOrderChanged(@NonNull List<PlaylistSummary> newOrder) {
            List<Playlist> playlists = new ArrayList<>();
            for (PlaylistSummary item : newOrder) {
                playlists.add(item.playlist);
            }
            PlaylistWriter.setPlaylistOrder(playlists);
        }
    }
}
