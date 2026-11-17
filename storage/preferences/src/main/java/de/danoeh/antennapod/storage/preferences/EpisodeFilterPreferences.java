package de.danoeh.antennapod.storage.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

public final class EpisodeFilterPreferences {
    public static final String PREF_EPISODE_FILTER_STYLE = "prefEpisodeFilterStyle";
    public static final String STYLE_BASIC = "basic";
    public static final String STYLE_ADVANCED = "advanced";

    private static SharedPreferences prefs;

    private EpisodeFilterPreferences() {
    }

    public static void init(@NonNull Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    }

    public static boolean isAdvanced() {
        return prefs != null && STYLE_ADVANCED.equals(prefs.getString(PREF_EPISODE_FILTER_STYLE, STYLE_BASIC));
    }
}
