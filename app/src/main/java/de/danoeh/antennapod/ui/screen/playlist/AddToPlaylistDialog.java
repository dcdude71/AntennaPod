package de.danoeh.antennapod.ui.screen.playlist;

import android.content.Context;
import android.util.Log;
import android.util.TypedValue;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.event.MessageEvent;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.playlist.Playlist;
import de.danoeh.antennapod.storage.database.PlaylistReader;
import de.danoeh.antennapod.storage.database.PlaylistWriter;
import de.danoeh.antennapod.ui.common.ThemeUtils;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public final class AddToPlaylistDialog {
    private static final String TAG = "AddToPlaylistDialog";

    private AddToPlaylistDialog() {
    }

    public static void show(@NonNull Context context, @NonNull List<FeedItem> items) {
        if (items.isEmpty()) {
            return;
        }
        final List<FeedItem> episodes = new ArrayList<>(items);
        Single.fromCallable(() -> {
            List<Playlist> manual = new ArrayList<>();
            for (Playlist playlist : PlaylistReader.getPlaylists()) {
                if (playlist.isManual()) {
                    manual.add(playlist);
                }
            }
            Set<Long> containing = episodes.size() == 1
                    ? PlaylistReader.getPlaylistIdsContaining(episodes.get(0).getId()) : Collections.emptySet();
            return new Pair<>(manual, containing);
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result.first.isEmpty()) {
                        createAndAdd(context, episodes);
                    } else {
                        showChoices(context, episodes, result.first, result.second);
                    }
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    private static void showChoices(@NonNull Context context, @NonNull List<FeedItem> episodes,
                                    @NonNull List<Playlist> playlists, @NonNull Set<Long> containing) {
        CharSequence[] names = new CharSequence[playlists.size()];
        boolean[] initial = new boolean[playlists.size()];
        for (int i = 0; i < playlists.size(); i++) {
            names[i] = playlists.get(i).getName();
            initial[i] = containing.contains(playlists.get(i).getId());
        }
        final boolean[] checked = initial.clone();
        TextView hint = new TextView(context);
        int padding = Math.round(24 * context.getResources().getDisplayMetrics().density);
        hint.setPadding(padding, padding / 3, padding, 0);
        hint.setText(R.string.add_to_playlist_only_manual);
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        hint.setTextColor(ThemeUtils.getColorFromAttr(context, android.R.attr.textColorSecondary));
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.add_to_playlist_title)
                .setMultiChoiceItems(names, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setView(hint)
                .setPositiveButton(R.string.add_to_playlist_done, (dialog, which) -> {
                    List<Long> addTo = new ArrayList<>();
                    List<Long> removeFrom = new ArrayList<>();
                    for (int i = 0; i < playlists.size(); i++) {
                        if (checked[i] && !initial[i]) {
                            addTo.add(playlists.get(i).getId());
                        } else if (!checked[i] && initial[i]) {
                            removeFrom.add(playlists.get(i).getId());
                        }
                    }
                    if (addTo.isEmpty() && removeFrom.isEmpty()) {
                        return;
                    }
                    PlaylistWriter.setMembership(episodes, addTo, removeFrom);
                    if (!addTo.isEmpty()) {
                        EventBus.getDefault().post(new MessageEvent(context.getResources().getQuantityString(
                                R.plurals.added_to_playlist_message, addTo.size(), addTo.size())));
                    }
                })
                .setNeutralButton(R.string.playlist_new, (dialog, which) -> createAndAdd(context, episodes))
                .setNegativeButton(R.string.cancel_label, null)
                .show();
    }

    private static void createAndAdd(@NonNull Context context, @NonNull List<FeedItem> episodes) {
        PlaylistNameDialog.show(context, R.string.add_to_playlist_new, "", name -> {
            Playlist playlist = new Playlist(name);
            playlist.setManual(true);
            Single.fromCallable(() -> {
                long id = PlaylistWriter.createPlaylist(playlist).get();
                PlaylistWriter.addEpisodes(id, episodes).get();
                return id;
            })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(id -> EventBus.getDefault().post(new MessageEvent(context.getResources()
                                    .getQuantityString(R.plurals.added_to_playlist_message, 1, 1))),
                            error -> Log.e(TAG, Log.getStackTraceString(error)));
        });
    }
}
