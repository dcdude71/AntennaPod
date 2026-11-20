package de.danoeh.antennapod.net.sync.wearinterface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.charset.StandardCharsets;

public final class CastlistWearAdSkip {
    public static final String AD_SKIP_PREFIX = "/castlist/ad_skip/";
    public static final String SKIP_AD = "/castlist/skip_ad";

    private CastlistWearAdSkip() {
    }

    @NonNull
    public static String adSkipPath(long itemId) {
        return AD_SKIP_PREFIX + itemId;
    }

    @Nullable
    public static Long parseAdSkipPath(@NonNull String path) {
        if (!path.startsWith(AD_SKIP_PREFIX)) {
            return null;
        }
        try {
            return Long.parseLong(path.substring(AD_SKIP_PREFIX.length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @NonNull
    public static byte[] secondsToBytes(int seconds) {
        return String.valueOf(seconds).getBytes(StandardCharsets.UTF_8);
    }

    public static int secondsFromBytes(@Nullable byte[] bytes) {
        if (bytes == null) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(new String(bytes, StandardCharsets.UTF_8).trim()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
