package de.danoeh.antennapod.playback.service.internal;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.CommandButton;
import androidx.media3.session.SessionCommand;

import com.google.common.collect.ImmutableList;

import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.playback.service.R;
import de.danoeh.antennapod.storage.database.FeedAdSkip;
import io.reactivex.rxjava3.schedulers.Schedulers;

public final class NotificationAdSkip {
    public static final String PREF_NOTIFICATION_AD_SKIP = "prefNotificationAdSkip";
    static final SessionCommand SESSION_COMMAND_SKIP_AD = new SessionCommand("skip_ad", Bundle.EMPTY);

    private static volatile int seconds = 0;
    private static volatile long feedId = -1;

    private NotificationAdSkip() {
    }

    public static void onPlayableChanged(@NonNull Context context, @Nullable FeedMedia media,
                                         @NonNull Runnable refreshNotification) {
        long newFeedId = media != null && media.getItem() != null ? media.getItem().getFeedId() : -1;
        Context appContext = context.getApplicationContext();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        Schedulers.io().scheduleDirect(() -> {
            int newSeconds = newFeedId < 0 ? 0 : FeedAdSkip.getSeconds(newFeedId);
            boolean changed = newSeconds != seconds || newFeedId != feedId;
            seconds = newSeconds;
            feedId = newFeedId;
            if (changed && isEnabled(appContext)) {
                mainHandler.post(refreshNotification);
            }
        });
    }

    @UnstableApi
    static void addButton(@NonNull Context context, @NonNull ImmutableList.Builder<CommandButton> buttons) {
        int skip = seconds;
        if (skip <= 0 || !isEnabled(context)) {
            return;
        }
        buttons.add(new CommandButton.Builder(CommandButton.ICON_UNDEFINED)
                .setSessionCommand(SESSION_COMMAND_SKIP_AD)
                .setCustomIconResId(R.drawable.ic_notification_skip_ad)
                .setDisplayName(context.getString(R.string.skip_ad_label, skip))
                .build());
    }

    static boolean handle(@NonNull SessionCommand command, @NonNull Player player) {
        if (!command.customAction.equals(SESSION_COMMAND_SKIP_AD.customAction)) {
            return false;
        }
        int skip = seconds;
        if (skip > 0) {
            long target = player.getCurrentPosition() + skip * 1000L;
            long duration = player.getDuration();
            player.seekTo(duration == C.TIME_UNSET ? target : Math.min(duration, target));
        }
        return true;
    }

    private static boolean isEnabled(@NonNull Context context) {
        return context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE)
                .getBoolean(PREF_NOTIFICATION_AD_SKIP, true);
    }
}
