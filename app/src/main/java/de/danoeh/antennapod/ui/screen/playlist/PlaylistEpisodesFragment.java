package de.danoeh.antennapod.ui.screen.playlist;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.actionbutton.ItemActionButton;
import de.danoeh.antennapod.actionbutton.PlayActionButton;
import de.danoeh.antennapod.actionbutton.StreamActionButton;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.event.FeedItemEvent;
import de.danoeh.antennapod.event.PlaylistEvent;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.playlist.Playlist;
import de.danoeh.antennapod.model.playlist.PlaylistRule;
import de.danoeh.antennapod.net.download.serviceinterface.AutoDownloadManager;
import de.danoeh.antennapod.storage.database.PlaylistPlayback;
import de.danoeh.antennapod.storage.database.PlaylistReader;
import de.danoeh.antennapod.storage.database.PlaylistWriter;
import de.danoeh.antennapod.ui.common.ConfirmationDialog;
import de.danoeh.antennapod.ui.episodeslist.EpisodeItemViewHolder;
import de.danoeh.antennapod.ui.episodeslist.EpisodesListFragment;

public class PlaylistEpisodesFragment extends EpisodesListFragment {
    public static final String TAG = "PlaylistEpisodesFragment";
    private static final String ARG_PLAYLIST_ID = "playlist_id";
    private static final String ARG_PREVIEW = "preview";
    private static final long JUST_ADDED_MILLIS = 2000;
    private static final int JUST_ADDED_COLOR = 0x5543A047;

    private long playlistId;
    private volatile Playlist playlist;
    private volatile int episodeCount;
    private volatile boolean hasUnlimitedRule;
    private volatile Set<Long> ruleFeedIds = Collections.emptySet();
    private boolean preview;
    private volatile Set<Long> previousEpisodeIds;
    private volatile Set<Long> justAddedIds = Collections.emptySet();
    private final Handler handler = new Handler(Looper.getMainLooper());

    public static PlaylistEpisodesFragment newInstance(long playlistId) {
        PlaylistEpisodesFragment fragment = new PlaylistEpisodesFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_PLAYLIST_ID, playlistId);
        fragment.setArguments(args);
        return fragment;
    }

    public static PlaylistEpisodesFragment newPreviewInstance(long playlistId) {
        PlaylistEpisodesFragment fragment = newInstance(playlistId);
        fragment.requireArguments().putBoolean(ARG_PREVIEW, true);
        return fragment;
    }

    long getPlaylistId() {
        return playlistId;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        playlistId = requireArguments().getLong(ARG_PLAYLIST_ID);
        preview = requireArguments().getBoolean(ARG_PREVIEW);
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);
        if (preview) {
            toolbar.setTitle(R.string.playlist_preview);
        } else {
            toolbar.inflateMenu(R.menu.playlist_episodes);
            toolbar.setTitle(R.string.playlists_label);
        }
        emptyView.setIcon(R.drawable.ic_playlist_play);
        emptyView.setTitle(R.string.playlist_episodes_empty_title);
        emptyView.setMessage(R.string.playlist_episodes_empty_message);
        return root;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.playlist_play) {
            playPlaylist();
            return true;
        } else if (itemId == R.id.playlist_edit_rules) {
            ((MainActivity) requireActivity()).loadChildFragment(PlaylistEditFragment.newInstance(playlistId));
            return true;
        } else if (itemId == R.id.playlist_auto_download) {
            Playlist current = playlist;
            if (current != null && !current.isAutoDownload() && hasUnlimitedRule) {
                showAutoDownloadNeedsLimitDialog();
                return true;
            }
            if (current != null) {
                current.setAutoDownload(!current.isAutoDownload());
                item.setChecked(current.isAutoDownload());
                Context context = requireContext().getApplicationContext();
                Future<?> update = PlaylistWriter.updatePlaylist(current);
                if (current.isAutoDownload()) {
                    Completable.fromFuture(update).subscribeOn(Schedulers.io()).subscribe(
                            () -> AutoDownloadManager.getInstance().autodownloadUndownloadedItems(context),
                            error -> Log.e(TAG, Log.getStackTraceString(error)));
                }
            }
            return true;
        } else if (itemId == R.id.playlist_android_auto) {
            Playlist current = playlist;
            if (current != null) {
                current.setShowInAndroidAuto(!current.isShowInAndroidAuto());
                item.setChecked(current.isShowInAndroidAuto());
                PlaylistWriter.updatePlaylist(current);
            }
            return true;
        } else if (itemId == R.id.playlist_rename) {
            Playlist current = playlist;
            if (current != null) {
                PlaylistNameDialog.show(requireContext(), R.string.playlist_rename, current.getName(), name -> {
                    current.setName(name);
                    PlaylistWriter.updatePlaylist(current);
                });
            }
            return true;
        } else if (itemId == R.id.playlist_delete) {
            confirmDelete();
            return true;
        }
        return super.onMenuItemClick(item);
    }

    private void playPlaylist() {
        if (episodes.isEmpty() || episodes.get(0).getMedia() == null) {
            return;
        }
        FeedItem first = episodes.get(0);
        PlaylistPlayback.start(requireContext(), playlistId);
        ItemActionButton button = first.getMedia().isDownloaded()
                ? new PlayActionButton(first) : new StreamActionButton(first);
        button.onClick(requireContext());
    }

    private void showAutoDownloadNeedsLimitDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.playlist_auto_download_needs_limit_title)
                .setMessage(R.string.playlist_auto_download_needs_limit_message)
                .setPositiveButton(R.string.playlist_edit_rules, (dialog, which) ->
                        ((MainActivity) requireActivity()).loadChildFragment(
                                PlaylistEditFragment.newInstance(playlistId)))
                .setNegativeButton(R.string.cancel_label, null)
                .show();
    }

    @Override
    protected void onEpisodeBound(EpisodeItemViewHolder holder) {
        holder.secondaryActionButton.setVisibility(View.GONE);
        FeedItem item = holder.getFeedItem();
        boolean justAdded = item != null && justAddedIds.contains(item.getId());
        holder.itemView.setForeground(justAdded ? new ColorDrawable(JUST_ADDED_COLOR) : null);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (!preview && getParentFragment() == null && !isHidden() && isResumed()
                && PlaylistPanes.isWide(requireContext())) {
            PlaylistPanes.setSelected(requireContext(), playlistId);
            getParentFragmentManager().popBackStack();
        }
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void confirmDelete() {
        Playlist current = playlist;
        if (current == null) {
            return;
        }
        new ConfirmationDialog(requireContext(), R.string.playlist_delete,
                getString(R.string.playlist_delete_confirm, current.getName())) {
            @Override
            public void onConfirmButtonPressed(DialogInterface dialog) {
                dialog.dismiss();
                if (PlaylistPlayback.getActivePlaylist(requireContext()) == playlistId) {
                    PlaylistPlayback.stop(requireContext());
                }
                PlaylistWriter.deletePlaylist(playlistId);
                if (getParentFragment() instanceof PlaylistsFragment) {
                    ((PlaylistsFragment) getParentFragment()).onSelectedPlaylistDeleted();
                } else {
                    getParentFragmentManager().popBackStack();
                }
            }
        }.createNewDialog().show();
    }

    @Override
    protected void updateToolbar() {
        if (preview) {
            toolbar.setSubtitle(getResources().getQuantityString(
                    R.plurals.playlist_preview_summary, episodeCount, episodeCount));
            return;
        }
        Playlist current = playlist;
        if (current != null) {
            toolbar.setTitle(current.getName());
            toolbar.getMenu().findItem(R.id.playlist_auto_download).setChecked(current.isAutoDownload());
            toolbar.getMenu().findItem(R.id.playlist_android_auto).setChecked(current.isShowInAndroidAuto());
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlaylistEvent(PlaylistEvent event) {
        if (event.playlistId == playlistId || event.playlistId == PlaylistWriter.ALL_PLAYLISTS) {
            loadItems();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFeedItemChanged(FeedItemEvent event) {
        Set<Long> feedIds = ruleFeedIds;
        for (FeedItem item : event.items) {
            if (item != null && feedIds.contains(item.getFeedId())) {
                loadItems();
                return;
            }
        }
    }

    @Override
    protected FeedItemFilter getFilter() {
        return FeedItemFilter.unfiltered();
    }

    @Override
    protected String getFragmentTag() {
        return TAG;
    }

    @NonNull
    @Override
    protected List<FeedItem> loadData() {
        playlist = PlaylistReader.getPlaylist(playlistId);
        boolean unlimited = false;
        Set<Long> feedIds = new HashSet<>();
        for (PlaylistRule rule : PlaylistReader.getRules(playlistId)) {
            if (rule.getEpisodeCount() == PlaylistRule.ALL_EPISODES) {
                unlimited = true;
            }
            feedIds.add(rule.getFeedId());
        }
        hasUnlimitedRule = unlimited;
        ruleFeedIds = feedIds;
        List<FeedItem> items = PlaylistReader.getEpisodes(playlistId);
        episodeCount = items.size();
        if (preview) {
            Set<Long> ids = new HashSet<>();
            for (FeedItem item : items) {
                ids.add(item.getId());
            }
            Set<Long> previous = previousEpisodeIds;
            previousEpisodeIds = ids;
            if (previous != null) {
                Set<Long> added = new HashSet<>(ids);
                added.removeAll(previous);
                if (!added.isEmpty()) {
                    justAddedIds = added;
                    handler.removeCallbacksAndMessages(null);
                    handler.postDelayed(this::clearJustAdded, JUST_ADDED_MILLIS);
                }
            }
        }
        return items;
    }

    private void clearJustAdded() {
        justAddedIds = Collections.emptySet();
        if (isAdded()) {
            loadItems();
        }
    }

    @NonNull
    @Override
    protected List<FeedItem> loadMoreData(int page) {
        return new ArrayList<>();
    }

    @Override
    protected int loadTotalItemCount() {
        return episodeCount;
    }
}
