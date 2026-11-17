package de.danoeh.antennapod.ui.screen.playlist;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.event.FeedItemEvent;
import de.danoeh.antennapod.event.FeedUpdateRunningEvent;
import de.danoeh.antennapod.event.PlaylistEvent;
import de.danoeh.antennapod.model.playlist.Playlist;
import de.danoeh.antennapod.storage.database.PlaylistReader;
import de.danoeh.antennapod.ui.appstartintent.PlaylistPlayStarter;
import de.danoeh.antennapod.ui.widget.PlaylistWidget;
import io.reactivex.rxjava3.schedulers.Schedulers;

public final class PlaylistShortcuts {
    private static final String TAG = "PlaylistShortcuts";
    private static final String ID_PREFIX = "playlist_";
    private static final int MAX_DYNAMIC_SHORTCUTS = 4;
    private static boolean registered = false;

    private final Context context;

    private PlaylistShortcuts(@NonNull Context context) {
        this.context = context;
    }

    public static synchronized void register(@NonNull Context context) {
        if (registered) {
            return;
        }
        registered = true;
        Context appContext = context.getApplicationContext();
        EventBus.getDefault().register(new PlaylistShortcuts(appContext));
        Schedulers.io().scheduleDirect(() -> update(appContext));
    }

    static boolean pin(@NonNull Context context, @NonNull Playlist playlist) {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            return false;
        }
        return ShortcutManagerCompat.requestPinShortcut(context, build(context, playlist, 0), null);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onPlaylistEvent(PlaylistEvent event) {
        PlaylistWidget.refresh(context);
        update(context);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onFeedItemEvent(FeedItemEvent event) {
        PlaylistWidget.refresh(context);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onFeedUpdateRunningEvent(FeedUpdateRunningEvent event) {
        if (!event.isFeedUpdateRunning) {
            PlaylistWidget.refresh(context);
        }
    }

    private static void update(@NonNull Context context) {
        try {
            List<Playlist> playlists = PlaylistReader.getPlaylists();
            List<ShortcutInfoCompat> shortcuts = new ArrayList<>();
            Set<String> ids = new HashSet<>();
            for (int i = 0; i < playlists.size(); i++) {
                ShortcutInfoCompat shortcut = build(context, playlists.get(i), i);
                shortcuts.add(shortcut);
                ids.add(shortcut.getId());
            }
            List<ShortcutInfoCompat> dynamic = shortcuts.subList(0, Math.min(MAX_DYNAMIC_SHORTCUTS, shortcuts.size()));
            ShortcutManagerCompat.setDynamicShortcuts(context, new ArrayList<>(dynamic));
            ShortcutManagerCompat.updateShortcuts(context, shortcuts);

            List<String> removed = new ArrayList<>();
            for (ShortcutInfoCompat pinned : ShortcutManagerCompat.getShortcuts(context,
                    ShortcutManagerCompat.FLAG_MATCH_PINNED)) {
                if (pinned.getId().startsWith(ID_PREFIX) && !ids.contains(pinned.getId())) {
                    removed.add(pinned.getId());
                }
            }
            if (!removed.isEmpty()) {
                ShortcutManagerCompat.disableShortcuts(context, removed,
                        context.getString(R.string.playlist_no_longer_exists));
            }
        } catch (RuntimeException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    @NonNull
    private static ShortcutInfoCompat build(@NonNull Context context, @NonNull Playlist playlist, int rank) {
        return new ShortcutInfoCompat.Builder(context, ID_PREFIX + playlist.getId())
                .setShortLabel(playlist.getName())
                .setLongLabel(playlist.getName())
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_mix_playlist))
                .setIntent(PlaylistPlayStarter.getIntent(context, playlist.getId()))
                .setRank(rank)
                .build();
    }
}
