package de.danoeh.antennapod.ui.screen.playback.audio;

import android.app.Activity;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.window.java.layout.WindowInfoTrackerCallbackAdapter;
import androidx.window.layout.DisplayFeature;
import androidx.window.layout.FoldingFeature;
import androidx.window.layout.WindowInfoTracker;
import androidx.window.layout.WindowLayoutInfo;

import de.danoeh.antennapod.R;

final class PlayerLayout implements Consumer<WindowLayoutInfo> {
    static final int WIDE_MIN_WIDTH_DP = 600;
    private static final String TAG_DESCRIPTION = "WideDescriptionFragment";

    private final AudioPlayerFragment fragment;
    private final View playerMain;
    private final View pager;
    private final View playtimeLayout;
    private final View descriptionPane;
    private final View shownotesButton;
    @Nullable
    private FoldingFeature tabletopFold;
    @Nullable
    private WindowInfoTrackerCallbackAdapter tracker;

    PlayerLayout(@NonNull AudioPlayerFragment fragment, @NonNull View root) {
        this.fragment = fragment;
        playerMain = root.findViewById(R.id.playerMain);
        pager = root.findViewById(R.id.pager);
        playtimeLayout = root.findViewById(R.id.playtime_layout);
        descriptionPane = root.findViewById(R.id.descriptionPane);
        shownotesButton = root.findViewById(R.id.shownotesButtonContainer);
    }

    void start() {
        Activity activity = fragment.getActivity();
        if (activity == null || tracker != null) {
            return;
        }
        tracker = new WindowInfoTrackerCallbackAdapter(WindowInfoTracker.getOrCreate(activity));
        tracker.addWindowLayoutInfoListener(activity, ContextCompat.getMainExecutor(activity), this);
    }

    void stop() {
        if (tracker != null) {
            tracker.removeWindowLayoutInfoListener(this);
            tracker = null;
        }
    }

    @Override
    public void accept(WindowLayoutInfo info) {
        FoldingFeature fold = null;
        for (DisplayFeature feature : info.getDisplayFeatures()) {
            if (feature instanceof FoldingFeature
                    && ((FoldingFeature) feature).getState() == FoldingFeature.State.HALF_OPENED
                    && ((FoldingFeature) feature).getOrientation() == FoldingFeature.Orientation.HORIZONTAL) {
                fold = (FoldingFeature) feature;
            }
        }
        tabletopFold = fold;
        apply();
    }

    void apply() {
        if (!fragment.isAdded()) {
            return;
        }
        boolean tabletop = tabletopFold != null;
        boolean wide = !tabletop
                && fragment.getResources().getConfiguration().screenWidthDp >= WIDE_MIN_WIDTH_DP;
        descriptionPane.setVisibility(wide ? View.VISIBLE : View.GONE);
        shownotesButton.setVisibility(wide ? View.GONE : View.VISIBLE);
        fragment.setCoverOnly(wide);
        updateDescriptionFragment(wide);
        if (tabletop) {
            playerMain.post(this::applyTabletop);
        } else {
            applyStacked();
        }
    }

    private void updateDescriptionFragment(boolean wide) {
        FragmentManager fragmentManager = fragment.getChildFragmentManager();
        Fragment description = fragmentManager.findFragmentByTag(TAG_DESCRIPTION);
        if (wide && description == null) {
            fragmentManager.beginTransaction()
                    .replace(R.id.descriptionPane, new ItemDescriptionFragment(), TAG_DESCRIPTION)
                    .commit();
        } else if (!wide && description != null) {
            fragmentManager.beginTransaction().remove(description).commit();
        }
    }

    private void applyTabletop() {
        FoldingFeature fold = tabletopFold;
        if (fold == null) {
            return;
        }
        int[] location = new int[2];
        playerMain.getLocationInWindow(location);
        int pagerHeight = fold.getBounds().top - location[1];
        if (pagerHeight <= 0) {
            applyStacked();
            return;
        }
        RelativeLayout.LayoutParams pagerParams = (RelativeLayout.LayoutParams) pager.getLayoutParams();
        pagerParams.height = pagerHeight;
        pagerParams.removeRule(RelativeLayout.ABOVE);
        pager.setLayoutParams(pagerParams);
        RelativeLayout.LayoutParams playtimeParams =
                (RelativeLayout.LayoutParams) playtimeLayout.getLayoutParams();
        playtimeParams.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        playtimeParams.addRule(RelativeLayout.BELOW, R.id.pager);
        playtimeParams.topMargin = fold.getBounds().height() + dp(16);
        playtimeLayout.setLayoutParams(playtimeParams);
    }

    private void applyStacked() {
        RelativeLayout.LayoutParams pagerParams = (RelativeLayout.LayoutParams) pager.getLayoutParams();
        pagerParams.height = 0;
        pagerParams.addRule(RelativeLayout.ABOVE, R.id.playtime_layout);
        pager.setLayoutParams(pagerParams);
        RelativeLayout.LayoutParams playtimeParams =
                (RelativeLayout.LayoutParams) playtimeLayout.getLayoutParams();
        playtimeParams.removeRule(RelativeLayout.BELOW);
        playtimeParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        playtimeParams.topMargin = 0;
        playtimeLayout.setLayoutParams(playtimeParams);
    }

    private int dp(int value) {
        return Math.round(value * fragment.getResources().getDisplayMetrics().density);
    }
}
