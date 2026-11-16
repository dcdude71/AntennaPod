package de.danoeh.antennapod.ui.screen.episode;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.MaterialToolbar;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.ui.common.ThemeUtils;

public final class EpisodeDetailPane {
    private static final int WIDE_MIN_WIDTH_DP = 600;
    private static final String TAG = "EpisodeDetailPane";

    private final AppCompatActivity activity;
    private final View mainView;
    private final View divider;
    private final FragmentContainerView pane;
    private final OnBackPressedCallback backCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            close();
        }
    };

    private EpisodeDetailPane(@NonNull AppCompatActivity activity, @NonNull View mainView,
                              @NonNull View divider, @NonNull FragmentContainerView pane) {
        this.activity = activity;
        this.mainView = mainView;
        this.divider = divider;
        this.pane = pane;
    }

    public static void install(@NonNull AppCompatActivity activity) {
        View mainView = activity.findViewById(R.id.main_content_view);
        ViewGroup parent = (ViewGroup) mainView.getParent();
        int index = parent.indexOfChild(mainView);
        final ViewGroup.LayoutParams mainParams = mainView.getLayoutParams();
        parent.removeViewAt(index);

        LinearLayout wrapper = new LinearLayout(activity);
        wrapper.setId(R.id.episodeDetailPaneWrapper);
        wrapper.setOrientation(LinearLayout.HORIZONTAL);
        wrapper.setBaselineAligned(false);
        parent.addView(wrapper, index, mainParams);
        wrapper.addView(mainView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));

        View divider = new View(activity);
        divider.setId(R.id.episodeDetailPaneDivider);
        divider.setBackgroundColor(ThemeUtils.getColorFromAttr(activity, R.attr.colorOutlineVariant));
        divider.setVisibility(View.GONE);
        int dividerWidth = Math.max(1, Math.round(activity.getResources().getDisplayMetrics().density));
        wrapper.addView(divider, new LinearLayout.LayoutParams(dividerWidth, ViewGroup.LayoutParams.MATCH_PARENT));

        FragmentContainerView pane = new FragmentContainerView(activity);
        pane.setId(R.id.episodeDetailPane);
        pane.setBackgroundColor(ThemeUtils.getColorFromAttr(activity, android.R.attr.colorBackground));
        pane.setVisibility(View.GONE);
        wrapper.addView(pane, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));

        EpisodeDetailPane detailPane = new EpisodeDetailPane(activity, mainView, divider, pane);
        wrapper.setTag(R.id.episodeDetailPaneWrapper, detailPane);
        mainView.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) ->
                v.post(detailPane::matchBottomMargin));
        activity.getSupportFragmentManager().registerFragmentLifecycleCallbacks(
                new FragmentManager.FragmentLifecycleCallbacks() {
                    @Override
                    public void onFragmentViewCreated(@NonNull FragmentManager fm, @NonNull Fragment f,
                                                      @NonNull View v, @Nullable Bundle savedInstanceState) {
                        if (f.getId() == R.id.episodeDetailPane) {
                            detailPane.useCloseButton(v);
                        }
                    }
                }, false);
        if (activity.getSupportFragmentManager().findFragmentById(R.id.episodeDetailPane) != null) {
            detailPane.setOpen(true);
        }
    }

    public static boolean show(@NonNull AppCompatActivity activity, @NonNull Fragment fragment) {
        EpisodeDetailPane detailPane = from(activity);
        if (detailPane == null || !(fragment instanceof ItemPagerFragment) || !isWide(activity)) {
            return false;
        }
        activity.getSupportFragmentManager().beginTransaction()
                .replace(R.id.episodeDetailPane, fragment, TAG)
                .commit();
        detailPane.setOpen(true);
        return true;
    }

    public static void close(@NonNull AppCompatActivity activity) {
        EpisodeDetailPane detailPane = from(activity);
        if (detailPane != null) {
            detailPane.close();
        }
    }

    public static void onConfigurationChanged(@NonNull AppCompatActivity activity) {
        EpisodeDetailPane detailPane = from(activity);
        if (detailPane == null || isWide(activity)) {
            return;
        }
        Fragment shown = activity.getSupportFragmentManager().findFragmentById(R.id.episodeDetailPane);
        detailPane.close();
        if (shown != null && shown.getArguments() != null) {
            ItemPagerFragment fullScreen = new ItemPagerFragment();
            fullScreen.setArguments(new Bundle(shown.getArguments()));
            ((MainActivity) activity).loadChildFragment(fullScreen);
        }
    }

    @Nullable
    private static EpisodeDetailPane from(@NonNull AppCompatActivity activity) {
        View wrapper = activity.findViewById(R.id.episodeDetailPaneWrapper);
        if (wrapper == null) {
            return null;
        }
        Object detailPane = wrapper.getTag(R.id.episodeDetailPaneWrapper);
        return detailPane instanceof EpisodeDetailPane ? (EpisodeDetailPane) detailPane : null;
    }

    private static boolean isWide(@NonNull AppCompatActivity activity) {
        return activity.getResources().getConfiguration().screenWidthDp >= WIDE_MIN_WIDTH_DP;
    }

    private void close() {
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        Fragment shown = fragmentManager.findFragmentById(R.id.episodeDetailPane);
        if (shown != null) {
            fragmentManager.beginTransaction().remove(shown).commit();
        }
        setOpen(false);
    }

    private void setOpen(boolean open) {
        pane.setVisibility(open ? View.VISIBLE : View.GONE);
        divider.setVisibility(open ? View.VISIBLE : View.GONE);
        backCallback.remove();
        backCallback.setEnabled(open);
        if (open) {
            activity.getOnBackPressedDispatcher().addCallback(activity, backCallback);
            matchBottomMargin();
        }
    }

    private void useCloseButton(@NonNull View fragmentView) {
        MaterialToolbar toolbar = fragmentView.findViewById(R.id.toolbar);
        if (toolbar == null) {
            return;
        }
        toolbar.setNavigationIcon(R.drawable.ic_close);
        toolbar.setNavigationContentDescription(R.string.close_label);
        toolbar.setNavigationOnClickListener(v -> close());
    }

    private void matchBottomMargin() {
        int bottom = ((ViewGroup.MarginLayoutParams) mainView.getLayoutParams()).bottomMargin;
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) pane.getLayoutParams();
        if (params.bottomMargin != bottom) {
            params.bottomMargin = bottom;
            pane.setLayoutParams(params);
            ViewGroup.MarginLayoutParams dividerParams = (ViewGroup.MarginLayoutParams) divider.getLayoutParams();
            dividerParams.bottomMargin = bottom;
            divider.setLayoutParams(dividerParams);
        }
    }
}
