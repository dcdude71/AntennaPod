package de.danoeh.antennapod.ui.screen.playlist;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

final class PlaylistPanes {
    static final long NO_PLAYLIST = 0;
    private static final int WIDE_MIN_WIDTH_DP = 600;
    private static final int FIRST_PANE_WIDTH_DP = 360;
    private static final String PREF_NAME = "PlaylistPanes";
    private static final String PREF_SELECTED = "selectedPlaylist";

    private PlaylistPanes() {
    }

    static boolean isWide(@NonNull Context context) {
        return context.getResources().getConfiguration().screenWidthDp >= WIDE_MIN_WIDTH_DP;
    }

    static void layoutPanes(@NonNull View first, @NonNull View divider, @NonNull View second, boolean wide) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) first.getLayoutParams();
        if (wide) {
            params.width = Math.round(FIRST_PANE_WIDTH_DP * first.getResources().getDisplayMetrics().density);
            params.weight = 0;
        } else {
            params.width = 0;
            params.weight = 1;
        }
        first.setLayoutParams(params);
        divider.setVisibility(wide ? View.VISIBLE : View.GONE);
        second.setVisibility(wide ? View.VISIBLE : View.GONE);
    }

    static long getSelected(@NonNull Context context) {
        return getPrefs(context).getLong(PREF_SELECTED, NO_PLAYLIST);
    }

    static void setSelected(@NonNull Context context, long playlistId) {
        getPrefs(context).edit().putLong(PREF_SELECTED, playlistId).apply();
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}
