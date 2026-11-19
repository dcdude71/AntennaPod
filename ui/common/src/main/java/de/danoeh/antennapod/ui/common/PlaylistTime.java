package de.danoeh.antennapod.ui.common;

import android.content.Context;

import androidx.annotation.NonNull;

public final class PlaylistTime {
    private static final long MINUTE_MILLIS = 60_000;

    private PlaylistTime() {
    }

    @NonNull
    public static String format(@NonNull Context context, long millis) {
        long minutes = Math.max(1, (millis + MINUTE_MILLIS / 2) / MINUTE_MILLIS);
        long hours = minutes / 60;
        if (hours == 0) {
            return context.getString(R.string.playlist_duration_minutes, minutes);
        }
        return context.getString(R.string.playlist_duration_hours_minutes, hours, minutes % 60);
    }
}
