package de.danoeh.antennapod.storage.preferences;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

public final class PlaylistPreferences {
    public static final String PREF_PLAYLIST_END_ACTION = "prefPlaylistEndAction";
    public static final String END_ACTION_STOP = "stop";
    public static final String END_ACTION_NEXT_PLAYLIST = "next_playlist";

    private PlaylistPreferences() {
    }

    public static boolean shouldContinueWithNextPlaylist(@NonNull Context context) {
        return END_ACTION_NEXT_PLAYLIST.equals(PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_PLAYLIST_END_ACTION, END_ACTION_NEXT_PLAYLIST));
    }
}
