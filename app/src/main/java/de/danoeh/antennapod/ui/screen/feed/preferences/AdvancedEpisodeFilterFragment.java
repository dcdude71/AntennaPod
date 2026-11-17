package de.danoeh.antennapod.ui.screen.feed.preferences;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.AdvancedEpisodeFilter;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.EpisodeFilters;
import de.danoeh.antennapod.ui.common.ThemeUtils;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class AdvancedEpisodeFilterFragment extends Fragment {
    public static final String TAG = "AdvancedEpisodeFilter";
    private static final String ARG_FEED_ID = "feed_id";
    private static final int PREVIEW_EPISODES = 50;
    private static final long PREVIEW_DELAY_MS = 250;

    private final CompositeDisposable disposables = new CompositeDisposable();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<String> includeTerms = new ArrayList<>();
    private final List<String> excludeTerms = new ArrayList<>();
    private final Runnable previewUpdater = this::updatePreview;
    private long feedId;
    private List<FeedItem> previewEpisodes = new ArrayList<>();
    private boolean loaded = false;

    private MaterialToolbar toolbar;
    private ChipGroup includeChips;
    private ChipGroup excludeChips;
    private TextInputEditText includeInput;
    private TextInputEditText excludeInput;
    private MaterialButtonToggleGroup matchToggle;
    private CheckBox searchTitle;
    private CheckBox searchDescription;
    private TextView noFieldWarning;
    private MaterialSwitch wholeWords;
    private TextInputEditText minDuration;
    private TextInputEditText maxDuration;
    private TextView previewSummary;
    private LinearLayout previewList;

    public static AdvancedEpisodeFilterFragment newInstance(long feedId) {
        AdvancedEpisodeFilterFragment fragment = new AdvancedEpisodeFilterFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_FEED_ID, feedId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.advanced_episode_filter_fragment, container, false);
        feedId = requireArguments().getLong(ARG_FEED_ID);
        toolbar = root.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());
        toolbar.getMenu().add(R.string.advanced_filter_save)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
                .setOnMenuItemClickListener(item -> {
                    save();
                    return true;
                });
        includeChips = root.findViewById(R.id.includeChips);
        excludeChips = root.findViewById(R.id.excludeChips);
        includeInput = root.findViewById(R.id.includeInput);
        excludeInput = root.findViewById(R.id.excludeInput);
        matchToggle = root.findViewById(R.id.matchToggle);
        searchTitle = root.findViewById(R.id.searchTitle);
        searchDescription = root.findViewById(R.id.searchDescription);
        noFieldWarning = root.findViewById(R.id.noFieldWarning);
        wholeWords = root.findViewById(R.id.wholeWords);
        minDuration = root.findViewById(R.id.minDuration);
        maxDuration = root.findViewById(R.id.maxDuration);
        previewSummary = root.findViewById(R.id.previewSummary);
        previewList = root.findViewById(R.id.previewList);

        setupWordInput(root.findViewById(R.id.includeInputLayout), includeInput, includeTerms, includeChips);
        setupWordInput(root.findViewById(R.id.excludeInputLayout), excludeInput, excludeTerms, excludeChips);
        matchToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> schedulePreview());
        searchTitle.setOnCheckedChangeListener((button, checked) -> schedulePreview());
        searchDescription.setOnCheckedChangeListener((button, checked) -> schedulePreview());
        wholeWords.setOnCheckedChangeListener((button, checked) -> schedulePreview());
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                schedulePreview();
            }
        };
        minDuration.addTextChangedListener(watcher);
        maxDuration.addTextChangedListener(watcher);
        load();
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(previewUpdater);
        disposables.clear();
    }

    private void load() {
        disposables.add(Single.fromCallable(() -> {
            Feed feed = DBReader.getFeed(feedId, false, 0, 0);
            if (feed == null) {
                throw new IllegalStateException("Feed " + feedId + " not found");
            }
            AdvancedEpisodeFilter filter = EpisodeFilters.getAdvancedFilterOrBasic(feed);
            List<FeedItem> episodes = DBReader.getFeedItemList(feed, new FeedItemFilter(FeedItemFilter.HAS_MEDIA),
                    SortOrder.DATE_NEW_OLD, 0, PREVIEW_EPISODES);
            EpisodeFilters.loadDescriptions(episodes);
            return new Object[] {feed, filter, episodes};
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    Feed feed = (Feed) result[0];
                    AdvancedEpisodeFilter filter = (AdvancedEpisodeFilter) result[1];
                    @SuppressWarnings("unchecked") List<FeedItem> episodes = (List<FeedItem>) result[2];
                    previewEpisodes = episodes;
                    toolbar.setSubtitle(feed.getTitle());
                    show(filter);
                    loaded = true;
                    updatePreview();
                }, error -> Log.e(TAG, Log.getStackTraceString(error))));
    }

    private void show(@NonNull AdvancedEpisodeFilter filter) {
        includeTerms.clear();
        includeTerms.addAll(filter.getIncludeTerms());
        excludeTerms.clear();
        excludeTerms.addAll(filter.getExcludeTerms());
        refreshChips(includeChips, includeTerms);
        refreshChips(excludeChips, excludeTerms);
        matchToggle.check(filter.isMatchAll() ? R.id.matchAllButton : R.id.matchAnyButton);
        searchTitle.setChecked(filter.isSearchTitle());
        searchDescription.setChecked(filter.isSearchDescription());
        wholeWords.setChecked(filter.isWholeWords());
        minDuration.setText(filter.getMinDurationSeconds() > 0
                ? String.valueOf(filter.getMinDurationSeconds() / 60) : "");
        maxDuration.setText(filter.getMaxDurationSeconds() > 0
                ? String.valueOf(filter.getMaxDurationSeconds() / 60) : "");
    }

    private void setupWordInput(@NonNull TextInputLayout layout, @NonNull TextInputEditText input,
                                @NonNull List<String> terms, @NonNull ChipGroup chips) {
        Runnable add = () -> {
            String term = input.getText() == null ? "" : input.getText().toString().trim();
            if (!term.isEmpty() && !terms.contains(term)) {
                terms.add(term);
                refreshChips(chips, terms);
                schedulePreview();
            }
            input.setText("");
        };
        layout.setEndIconOnClickListener(v -> add.run());
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                add.run();
                return true;
            }
            return false;
        });
    }

    private void refreshChips(@NonNull ChipGroup chips, @NonNull List<String> terms) {
        chips.removeAllViews();
        for (String term : terms) {
            Chip chip = new Chip(requireContext());
            chip.setText(term);
            chip.setCloseIconVisible(true);
            chip.setCloseIconContentDescription(getString(R.string.advanced_filter_remove_word, term));
            chip.setOnCloseIconClickListener(v -> {
                terms.remove(term);
                refreshChips(chips, terms);
                schedulePreview();
            });
            chips.addView(chip);
        }
    }

    @NonNull
    private AdvancedEpisodeFilter currentFilter() {
        return new AdvancedEpisodeFilter(includeTerms, matchToggle.getCheckedButtonId() == R.id.matchAllButton,
                excludeTerms, searchTitle.isChecked(), searchDescription.isChecked(), wholeWords.isChecked(),
                minutesToSeconds(minDuration), minutesToSeconds(maxDuration));
    }

    private static int minutesToSeconds(@NonNull TextInputEditText input) {
        String text = input.getText() == null ? "" : input.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            return AdvancedEpisodeFilter.NO_LIMIT;
        }
        try {
            int minutes = Integer.parseInt(text);
            return minutes > 0 ? minutes * 60 : AdvancedEpisodeFilter.NO_LIMIT;
        } catch (NumberFormatException e) {
            return AdvancedEpisodeFilter.NO_LIMIT;
        }
    }

    private void schedulePreview() {
        if (!loaded) {
            return;
        }
        handler.removeCallbacks(previewUpdater);
        handler.postDelayed(previewUpdater, PREVIEW_DELAY_MS);
    }

    private void updatePreview() {
        if (getContext() == null) {
            return;
        }
        boolean noField = !searchTitle.isChecked() && !searchDescription.isChecked();
        noFieldWarning.setVisibility(noField ? View.VISIBLE : View.GONE);
        AdvancedEpisodeFilter filter = currentFilter();
        previewList.removeAllViews();
        if (previewEpisodes.isEmpty()) {
            previewSummary.setText(R.string.advanced_filter_preview_empty);
            return;
        }
        int matching = 0;
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        int accent = ThemeUtils.getColorFromAttr(requireContext(), androidx.appcompat.R.attr.colorPrimary);
        int onAccent = ThemeUtils.getColorFromAttr(requireContext(),
                com.google.android.material.R.attr.colorOnPrimary);
        int muted = ThemeUtils.getColorFromAttr(requireContext(),
                com.google.android.material.R.attr.colorSurfaceVariant);
        int onMuted = ThemeUtils.getColorFromAttr(requireContext(),
                com.google.android.material.R.attr.colorOnSurfaceVariant);
        for (FeedItem episode : previewEpisodes) {
            int duration = episode.getMedia() != null ? episode.getMedia().getDuration() : 0;
            AdvancedEpisodeFilter.Result result = filter.evaluate(episode.getTitle(),
                    episode.getDescription(), duration);
            if (result.matches) {
                matching++;
            }
            View row = inflater.inflate(R.layout.advanced_episode_filter_preview_item, previewList, false);
            TextView badge = row.findViewById(R.id.badge);
            badge.setText(result.matches ? "✓" : "–");
            badge.setTextColor(result.matches ? onAccent : onMuted);
            badge.setBackground(circle(result.matches ? accent : muted));
            TextView title = row.findViewById(R.id.title);
            title.setText(episode.getTitle());
            title.setAlpha(result.matches ? 1f : 0.7f);
            ((TextView) row.findViewById(R.id.reason)).setText(reason(result));
            previewList.addView(row);
        }
        previewSummary.setText(getString(R.string.advanced_filter_preview_summary, matching,
                previewEpisodes.size()));
    }

    @NonNull
    private static GradientDrawable circle(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        return drawable;
    }

    @NonNull
    private String reason(@NonNull AdvancedEpisodeFilter.Result result) {
        switch (result.reason) {
            case INCLUDED:
                return getString(result.inDescription ? R.string.advanced_filter_reason_description_has
                        : R.string.advanced_filter_reason_title_has, result.term);
            case EXCLUDED:
                return getString(result.inDescription ? R.string.advanced_filter_reason_skipped_description
                        : R.string.advanced_filter_reason_skipped_title, result.term);
            case MISSING_WORD:
                return getString(R.string.advanced_filter_reason_missing, result.term);
            case NO_WORD_FOUND:
                return getString(R.string.advanced_filter_reason_none);
            case TOO_SHORT:
                return getString(R.string.advanced_filter_reason_too_short);
            case TOO_LONG:
                return getString(R.string.advanced_filter_reason_too_long);
            case NO_WORDS:
            default:
                return getString(R.string.advanced_filter_reason_no_words);
        }
    }

    private void save() {
        AdvancedEpisodeFilter filter = currentFilter();
        disposables.add(Completable.fromAction(() -> EpisodeFilters.setAdvancedFilter(feedId, filter))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    if (isAdded()) {
                        getParentFragmentManager().popBackStack();
                    }
                }, error -> Log.e(TAG, Log.getStackTraceString(error))));
    }
}
