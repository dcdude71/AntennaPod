package de.danoeh.antennapod.ui.screen.subscriptions;

import android.content.Context;

import androidx.annotation.NonNull;

final class SubscriptionColumns {
    private static final int WIDE_SCREEN_DP = 600;

    private SubscriptionColumns() {
    }

    static int shown(@NonNull Context context, int chosen) {
        return context.getResources().getConfiguration().screenWidthDp >= WIDE_SCREEN_DP ? chosen * 2 : chosen;
    }
}
