package de.danoeh.antennapod.ui.screen.feed.preferences;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.model.feed.AdvancedEpisodeFilter;
import de.danoeh.antennapod.model.feed.EpisodeCondition;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.storage.database.EpisodeFilters;
import de.danoeh.antennapod.storage.preferences.EpisodeFilterPreferences;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public final class AdvancedEpisodeFilterSettings {
    private static final String TAG = "AdvancedFilterSettings";

    private AdvancedEpisodeFilterSettings() {
    }

    public static void setup(@NonNull PreferenceFragmentCompat fragment, @Nullable Preference preference,
                             @NonNull Feed feed) {
        if (preference == null) {
            return;
        }
        Preference.OnPreferenceClickListener basicListener = preference.getOnPreferenceClickListener();
        final CharSequence basicSummary = preference.getSummary();
        preference.setOnPreferenceClickListener(p -> {
            if (EpisodeFilterPreferences.isAdvanced() && fragment.getActivity() instanceof MainActivity) {
                ((MainActivity) fragment.getActivity()).loadChildFragment(
                        AdvancedEpisodeFilterFragment.newInstance(feed.getId()));
                return false;
            }
            return basicListener == null || basicListener.onPreferenceClick(p);
        });
        Runnable refresh = () -> updateSummary(fragment, preference, feed, basicSummary);
        refresh.run();
        FragmentManager fragmentManager = fragment.getParentFragmentManager();
        FragmentManager.OnBackStackChangedListener listener = refresh::run;
        fragmentManager.addOnBackStackChangedListener(listener);
        fragment.getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onDestroy(@NonNull LifecycleOwner owner) {
                fragmentManager.removeOnBackStackChangedListener(listener);
            }
        });
    }

    private static void updateSummary(@NonNull PreferenceFragmentCompat fragment, @NonNull Preference preference,
                                      @NonNull Feed feed, @Nullable CharSequence basicSummary) {
        if (!EpisodeFilterPreferences.isAdvanced()) {
            preference.setSummary(basicSummary);
            return;
        }
        Context context = fragment.getContext();
        if (context == null) {
            return;
        }
        Single.fromCallable(() -> EpisodeFilters.getAdvancedFilterOrBasic(feed))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(filter -> preference.setSummary(summarize(context, filter)),
                        error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    @NonNull
    static String summarize(@NonNull Context context, @NonNull AdvancedEpisodeFilter filter) {
        if (filter.isEmpty()) {
            return context.getString(R.string.advanced_filter_summary_none);
        }
        List<String> parts = new ArrayList<>();
        if (!filter.getKeepConditions().isEmpty()) {
            parts.add(context.getString(filter.isKeepMatchAll() ? R.string.advanced_filter_summary_keep_all
                    : R.string.advanced_filter_summary_keep_any, describe(context, filter.getKeepConditions())));
        }
        if (!filter.getSkipConditions().isEmpty()) {
            parts.add(context.getString(filter.isSkipMatchAll() ? R.string.advanced_filter_summary_skip_all
                    : R.string.advanced_filter_summary_skip_any, describe(context, filter.getSkipConditions())));
        }
        return TextUtils.join(" · ", parts);
    }

    @NonNull
    private static String describe(@NonNull Context context, @NonNull List<EpisodeCondition> conditions) {
        List<String> descriptions = new ArrayList<>();
        for (EpisodeCondition condition : conditions) {
            descriptions.add(describe(context, condition));
        }
        return TextUtils.join(", ", descriptions);
    }

    @NonNull
    static String describe(@NonNull Context context, @NonNull EpisodeCondition condition) {
        String operator = context.getString(operatorLabel(condition.getOperator()));
        if (condition.getField() == EpisodeCondition.Field.LENGTH) {
            return context.getString(R.string.advanced_filter_condition_length, operator, condition.getMinutes());
        }
        int field;
        switch (condition.getField()) {
            case TITLE:
                field = R.string.advanced_filter_condition_title;
                break;
            case DESCRIPTION:
                field = R.string.advanced_filter_condition_description;
                break;
            case POST:
            default:
                field = R.string.advanced_filter_condition_post;
                break;
        }
        String text = context.getString(R.string.advanced_filter_condition_text,
                context.getString(field), operator, condition.getValue());
        return condition.isWholeWord() ? context.getString(R.string.advanced_filter_condition_whole_word, text) : text;
    }

    @StringRes
    static int fieldLabel(@NonNull EpisodeCondition.Field field) {
        switch (field) {
            case TITLE:
                return R.string.advanced_filter_field_title;
            case DESCRIPTION:
                return R.string.advanced_filter_field_description;
            case LENGTH:
                return R.string.advanced_filter_field_length;
            case POST:
            default:
                return R.string.advanced_filter_field_post;
        }
    }

    @StringRes
    static int operatorLabel(@NonNull EpisodeCondition.Operator operator) {
        switch (operator) {
            case NOT_CONTAINS:
                return R.string.advanced_filter_op_not_contains;
            case STARTS_WITH:
                return R.string.advanced_filter_op_starts_with;
            case ENDS_WITH:
                return R.string.advanced_filter_op_ends_with;
            case IS:
                return R.string.advanced_filter_op_is;
            case SHORTER_THAN:
                return R.string.advanced_filter_op_shorter_than;
            case LONGER_THAN:
                return R.string.advanced_filter_op_longer_than;
            case CONTAINS:
            default:
                return R.string.advanced_filter_op_contains;
        }
    }
}
