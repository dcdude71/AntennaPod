package de.danoeh.antennapod.ui.appstartintent;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public final class PlaylistPlayStarter {
    public static final String INTENT = "de.danoeh.antennapod.intents.PLAY_PLAYLIST";
    public static final String EXTRA_PLAYLIST_ID = "playlist_id";

    private PlaylistPlayStarter() {
    }

    public static Intent getIntent(Context context, long playlistId) {
        Intent intent = new Intent(INTENT);
        intent.setPackage(context.getPackageName());
        intent.putExtra(EXTRA_PLAYLIST_ID, playlistId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        return intent;
    }

    public static PendingIntent getPendingIntent(Context context, long playlistId) {
        return PendingIntent.getActivity(context, (int) playlistId, getIntent(context, playlistId),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
