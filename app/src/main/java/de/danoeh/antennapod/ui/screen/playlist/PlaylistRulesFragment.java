package de.danoeh.antennapod.ui.screen.playlist;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.event.PlaylistEvent;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.playlist.Playlist;
import de.danoeh.antennapod.model.playlist.PlaylistRule;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.PlaylistReader;
import de.danoeh.antennapod.storage.database.PlaylistWriter;
import de.danoeh.antennapod.ui.common.EmptyViewHandler;
import de.danoeh.antennapod.ui.common.ImagePlaceholder;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class PlaylistRulesFragment extends Fragment implements Toolbar.OnMenuItemClickListener {
    public static final String TAG = "PlaylistRulesFragment";
    private static final String ARG_PLAYLIST_ID = "playlist_id";

    private long playlistId;
    private RuleAdapter adapter;
    private EmptyViewHandler emptyView;
    private Disposable disposable;
    private final Map<Long, Feed> feeds = new HashMap<>();
    private volatile boolean autoDownload;
    private volatile String playlistName = "";
    private volatile int episodeCount;
    private MaterialToolbar toolbar;

    public static PlaylistRulesFragment newInstance(long playlistId) {
        PlaylistRulesFragment fragment = new PlaylistRulesFragment();
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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.playlist_list_fragment, container, false);
        toolbar = root.findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.playlist_edit_rules);
        toolbar.inflateMenu(R.menu.playlist_rules);
        toolbar.setOnMenuItemClickListener(this);
        ((MainActivity) getActivity()).setupToolbarToggle(toolbar, true);

        RecyclerView recyclerView = root.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RuleAdapter();
        adapter.attachTo(recyclerView, true);

        emptyView = new EmptyViewHandler(getContext());
        emptyView.setIcon(R.drawable.ic_feed);
        emptyView.setTitle(R.string.playlist_rules_empty_title);
        emptyView.setMessage(R.string.playlist_rules_empty_message);
        emptyView.attachToRecyclerView(recyclerView);
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        loadRules();
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        if (disposable != null) {
            disposable.dispose();
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.playlist_add_podcast) {
            showPodcastPicker();
            return true;
        }
        return false;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlaylistEvent(PlaylistEvent event) {
        if (event.playlistId == playlistId) {
            loadRules();
        }
    }

    private void loadRules() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Observable.fromCallable(() -> {
            Map<Long, Feed> feedMap = new HashMap<>();
            for (Feed feed : DBReader.getFeedList()) {
                feedMap.put(feed.getId(), feed);
            }
            Playlist playlist = PlaylistReader.getPlaylist(playlistId);
            autoDownload = playlist != null && playlist.isAutoDownload();
            playlistName = playlist != null ? playlist.getName() : "";
            episodeCount = PlaylistReader.getEpisodes(playlistId).size();
            return new Pair<>(feedMap, PlaylistReader.getRules(playlistId));
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    feeds.clear();
                    feeds.putAll(result.first);
                    adapter.setItems(result.second);
                    emptyView.updateVisibility();
                    updateSubtitle();
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    void updateSubtitle() {
        if (toolbar == null) {
            return;
        }
        Fragment parent = getParentFragment();
        if (parent instanceof PlaylistEditFragment && ((PlaylistEditFragment) parent).isPreviewShown()) {
            toolbar.setSubtitle(playlistName);
        } else {
            toolbar.setSubtitle(getResources().getQuantityString(
                    R.plurals.playlist_rules_preview_summary, episodeCount, episodeCount));
        }
    }

    private void showPodcastPicker() {
        List<Feed> subscribed = new ArrayList<>();
        for (Feed feed : feeds.values()) {
            if (feed.getState() == Feed.STATE_SUBSCRIBED) {
                subscribed.add(feed);
            }
        }
        Collections.sort(subscribed, (a, b) -> String.CASE_INSENSITIVE_ORDER.compare(
                String.valueOf(a.getTitle()), String.valueOf(b.getTitle())));
        String[] titles = new String[subscribed.size()];
        for (int i = 0; i < subscribed.size(); i++) {
            titles[i] = subscribed.get(i).getTitle();
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.playlist_choose_podcast)
                .setItems(titles, (dialog, which) -> {
                    Feed feed = subscribed.get(which);
                    PlaylistRuleDialog.show(requireContext(), feed.getTitle(),
                            new PlaylistRule(playlistId, feed.getId()), autoDownload, PlaylistWriter::addRule);
                })
                .setNegativeButton(R.string.cancel_label, null)
                .show();
    }

    private class RuleAdapter extends ReorderableListAdapter<PlaylistRule> {
        @Override
        void bind(@NonNull ViewHolder holder, @NonNull PlaylistRule rule) {
            Context context = holder.itemView.getContext();
            Feed feed = feeds.get(rule.getFeedId());
            holder.title.setText(feed != null ? feed.getTitle() : "");
            holder.subtitle.setText(PlaylistRuleDialog.getSummary(context, rule));
            float radius = 4 * context.getResources().getDisplayMetrics().density;
            Glide.with(context)
                    .load(feed != null ? feed.getImageUrl() : null)
                    .apply(new RequestOptions()
                            .placeholder(ImagePlaceholder.getDrawable(context, radius))
                            .error(ImagePlaceholder.getDrawable(context, radius))
                            .transform(new FitCenter(), new RoundedCorners((int) radius))
                            .dontAnimate())
                    .into(holder.image);
        }

        @Override
        void onItemClick(@NonNull PlaylistRule rule) {
            Feed feed = feeds.get(rule.getFeedId());
            PlaylistRuleDialog.show(requireContext(), feed != null ? feed.getTitle() : "",
                    rule, autoDownload, PlaylistWriter::updateRule);
        }

        @Override
        void onOrderChanged(@NonNull List<PlaylistRule> newOrder) {
            PlaylistWriter.setRuleOrder(playlistId, newOrder);
        }

        @Override
        void onItemSwiped(@NonNull PlaylistRule rule) {
            PlaylistWriter.deleteRule(rule);
        }
    }
}
