package de.danoeh.antennapod.ui.screen.playlist;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.actionbutton.ItemActionButton;
import de.danoeh.antennapod.actionbutton.PlayActionButton;
import de.danoeh.antennapod.actionbutton.StreamActionButton;
import de.danoeh.antennapod.event.PlayerStatusEvent;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.playback.service.PlaybackServiceInterface;
import de.danoeh.antennapod.storage.database.PlaylistPlayback;
import de.danoeh.antennapod.storage.database.PlaylistReader;
import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;
import de.danoeh.antennapod.ui.appstartintent.PlaylistPlayStarter;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class PlaylistPlayActivity extends Activity {
    private static final String TAG = "PlaylistPlayActivity";
    private static final int MISSING = 0;
    private static final int EMPTY = 1;
    private static final int READY = 2;

    private static final long MAX_WAIT_MS = 10000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Disposable disposable;
    private long startedMediaId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        long playlistId = getIntent().getLongExtra(PlaylistPlayStarter.EXTRA_PLAYLIST_ID,
                PlaylistPlayback.NO_PLAYLIST);
        final FeedItem[] first = new FeedItem[1];
        disposable = Single.fromCallable(() -> {
            if (PlaylistReader.getPlaylist(playlistId) == null) {
                return MISSING;
            }
            List<FeedItem> episodes = PlaylistReader.getEpisodes(playlistId);
            if (episodes.isEmpty() || episodes.get(0).getMedia() == null) {
                return EMPTY;
            }
            first[0] = episodes.get(0);
            return READY;
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result == READY) {
                        PlaylistPlayback.start(this, playlistId);
                        ItemActionButton button = first[0].getMedia().isDownloaded()
                                ? new PlayActionButton(first[0]) : new StreamActionButton(first[0]);
                        button.onClick(this);
                        waitForPlayback(first[0].getMedia().getId());
                        return;
                    } else {
                        Toast.makeText(getApplicationContext(), result == MISSING
                                ? R.string.playlist_no_longer_exists : R.string.playlist_episodes_empty_title,
                                Toast.LENGTH_SHORT).show();
                    }
                    finish();
                }, error -> {
                    Log.e(TAG, Log.getStackTraceString(error));
                    finish();
                });
    }

    private void waitForPlayback(long mediaId) {
        startedMediaId = mediaId;
        EventBus.getDefault().register(this);
        handler.postDelayed(this::finish, MAX_WAIT_MS);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayerStatusChanged(PlayerStatusEvent event) {
        if (PlaybackServiceInterface.isPlaying
                && PlaybackPreferences.getCurrentlyPlayingFeedMediaId() == startedMediaId) {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        EventBus.getDefault().unregister(this);
        if (disposable != null) {
            disposable.dispose();
        }
    }
}
