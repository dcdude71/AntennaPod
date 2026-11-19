package de.danoeh.antennapod.event;

import androidx.annotation.Nullable;

public class PlaylistTransitionEvent {
    @Nullable
    public final String nextPlaylistName;

    public PlaylistTransitionEvent(@Nullable String nextPlaylistName) {
        this.nextPlaylistName = nextPlaylistName;
    }
}
