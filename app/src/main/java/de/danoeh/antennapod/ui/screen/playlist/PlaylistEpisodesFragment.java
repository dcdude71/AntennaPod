package de.danoeh.antennapod.ui.screen.playlist;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

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
import de.danoeh.antennapod.storage.database.PlaylistPlayback;
import de.danoeh.antennapod.storage.database.PlaylistReader;
import de.danoeh.antennapod.storage.database.PlaylistWriter;
import de.danoeh.antennapod.ui.common.ConfirmationDialog;
import de.danoeh.antennapod.ui.episodeslist.EpisodesListFragment;

public class PlaylistEpisodesFragment extends EpisodesListFragment {
    public static final String TAG = "PlaylistEpisodesFragment";
    private static final String ARG_PLAYLIST_ID = "playlist_id";

    private long playlistId;
    private volatile Playlist playlist;
    private volatile int episodeCount;

    public static PlaylistEpisodesFragment newInstance(long playlistId) {
        PlaylistEpisodesFragment fragment = new PlaylistEpisodesFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_PLAYLIST_ID, playlistId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        playlistId = requireArguments().getLong(ARG_PLAYLIST_ID);
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);
        toolbar.inflateMenu(R.menu.playlist_episodes);
        toolbar.setTitle(R.string.playlists_label);
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
            ((MainActivity) requireActivity()).loadChildFragment(PlaylistRulesFragment.newInstance(playlistId));
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
                getParentFragmentManager().popBackStack();
            }
        }.createNewDialog().show();
    }

    @Override
    protected void updateToolbar() {
        Playlist current = playlist;
        if (current != null) {
            toolbar.setTitle(current.getName());
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
        loadItems();
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
        List<FeedItem> items = PlaylistReader.getEpisodes(playlistId);
        episodeCount = items.size();
        return items;
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
