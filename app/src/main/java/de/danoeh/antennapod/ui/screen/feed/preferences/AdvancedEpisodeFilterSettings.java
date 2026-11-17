package de.danoeh.antennapod.ui.screen.feed.preferences;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
        if (!filter.getIncludeTerms().isEmpty()) {
            parts.add(context.getString(filter.isMatchAll() ? R.string.advanced_filter_summary_include_all
                    : R.string.advanced_filter_summary_include_any, quoted(filter.getIncludeTerms())));
        }
        if (!filter.getExcludeTerms().isEmpty()) {
            parts.add(context.getString(R.string.advanced_filter_summary_exclude, quoted(filter.getExcludeTerms())));
        }
        if (!filter.getIncludeTerms().isEmpty() || !filter.getExcludeTerms().isEmpty()) {
            if (filter.isSearchDescription() && filter.isSearchTitle()) {
                parts.add(context.getString(R.string.advanced_filter_summary_title_description));
            } else if (filter.isSearchDescription()) {
                parts.add(context.getString(R.string.advanced_filter_summary_description));
            }
            if (filter.isWholeWords()) {
                parts.add(context.getString(R.string.advanced_filter_summary_whole_words));
            }
        }
        int min = filter.getMinDurationSeconds() / 60;
        int max = filter.getMaxDurationSeconds() / 60;
        if (min > 0 && max > 0) {
            parts.add(context.getString(R.string.advanced_filter_summary_range, min, max));
        } else if (min > 0) {
            parts.add(context.getString(R.string.advanced_filter_summary_min, min));
        } else if (max > 0) {
            parts.add(context.getString(R.string.advanced_filter_summary_max, max));
        }
        return TextUtils.join(" · ", parts);
    }

    @NonNull
    private static String quoted(@NonNull List<String> terms) {
        List<String> quoted = new ArrayList<>();
        for (String term : terms) {
            quoted.add("“" + term + "”");
        }
        return TextUtils.join(", ", quoted);
    }
}
