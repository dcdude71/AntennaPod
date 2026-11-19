package de.danoeh.antennapod.ui.screen.feed.preferences;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.AdvancedEpisodeFilter;
import de.danoeh.antennapod.model.feed.EpisodeCondition;
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
    private static final EpisodeCondition.Field[] FIELDS = EpisodeCondition.Field.values();
    private static final EpisodeCondition.Operator[] TEXT_OPERATORS = {
        EpisodeCondition.Operator.CONTAINS, EpisodeCondition.Operator.NOT_CONTAINS,
        EpisodeCondition.Operator.STARTS_WITH, EpisodeCondition.Operator.ENDS_WITH,
        EpisodeCondition.Operator.IS
    };
    private static final EpisodeCondition.Operator[] LENGTH_OPERATORS = {
        EpisodeCondition.Operator.SHORTER_THAN, EpisodeCondition.Operator.LONGER_THAN
    };

    private final CompositeDisposable disposables = new CompositeDisposable();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable previewUpdater = this::updatePreview;
    private long feedId;
    private List<FeedItem> previewEpisodes = new ArrayList<>();
    private boolean loaded = false;

    private MaterialToolbar toolbar;
    private Section keepSection;
    private Section skipSection;
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
        toolbar.getMenu().add(R.string.advanced_filter_copy)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER)
                .setOnMenuItemClickListener(item -> {
                    chooseFilterToCopy();
                    return true;
                });
        keepSection = new Section(root.findViewById(R.id.keepMatchToggle), R.id.keepMatchAllButton,
                R.id.keepMatchAnyButton, root.findViewById(R.id.keepEmpty), root.findViewById(R.id.keepConditions));
        skipSection = new Section(root.findViewById(R.id.skipMatchToggle), R.id.skipMatchAllButton,
                R.id.skipMatchAnyButton, root.findViewById(R.id.skipEmpty), root.findViewById(R.id.skipConditions));
        root.findViewById(R.id.keepAddButton).setOnClickListener(v -> keepSection.addRow(null, true));
        root.findViewById(R.id.skipAddButton).setOnClickListener(v -> skipSection.addRow(null, true));
        previewSummary = root.findViewById(R.id.previewSummary);
        previewList = root.findViewById(R.id.previewList);
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
        keepSection.show(filter.getKeepConditions(), filter.isKeepMatchAll());
        skipSection.show(filter.getSkipConditions(), filter.isSkipMatchAll());
    }

    private void chooseFilterToCopy() {
        disposables.add(Single.fromCallable(() -> {
            List<Feed> feeds = new ArrayList<>();
            for (Feed feed : DBReader.getFeedList()) {
                if (feed.getId() != feedId) {
                    feeds.add(feed);
                }
            }
            return feeds;
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(feeds -> {
                    String[] titles = new String[feeds.size()];
                    for (int i = 0; i < feeds.size(); i++) {
                        titles[i] = feeds.get(i).getTitle();
                    }
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.advanced_filter_copy_choose)
                            .setItems(titles, (dialog, which) -> copyFilterOf(feeds.get(which)))
                            .setNegativeButton(R.string.cancel_label, null)
                            .show();
                }, error -> Log.e(TAG, Log.getStackTraceString(error))));
    }

    private void copyFilterOf(@NonNull Feed feed) {
        disposables.add(Single.fromCallable(() -> EpisodeFilters.getAdvancedFilterOrBasic(feed))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(filter -> {
                    show(filter);
                    schedulePreview();
                }, error -> Log.e(TAG, Log.getStackTraceString(error))));
    }

    @NonNull
    private AdvancedEpisodeFilter currentFilter() {
        return new AdvancedEpisodeFilter(keepSection.conditions(), keepSection.isMatchAll(),
                skipSection.conditions(), skipSection.isMatchAll());
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
        String condition = result.condition != null
                ? AdvancedEpisodeFilterSettings.describe(requireContext(), result.condition) : null;
        switch (result.reason) {
            case KEPT:
                return condition != null ? getString(R.string.advanced_filter_reason_kept, condition)
                        : getString(R.string.advanced_filter_reason_kept_all);
            case NOT_KEPT:
                return condition != null ? getString(R.string.advanced_filter_reason_not_kept, condition)
                        : getString(R.string.advanced_filter_reason_not_kept_any);
            case SKIPPED:
                return condition != null ? getString(R.string.advanced_filter_reason_skipped, condition)
                        : getString(R.string.advanced_filter_reason_skipped_all);
            case NOT_SKIPPED:
                return getString(R.string.advanced_filter_reason_not_skipped);
            case NO_CONDITIONS:
            default:
                return getString(R.string.advanced_filter_reason_no_filter);
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

    private class Section {
        private final MaterialButtonToggleGroup matchToggle;
        private final int matchAllId;
        private final int matchAnyId;
        private final TextView emptyView;
        private final LinearLayout container;
        private final List<ConditionRow> rows = new ArrayList<>();

        Section(MaterialButtonToggleGroup matchToggle, int matchAllId, int matchAnyId, TextView emptyView,
                LinearLayout container) {
            this.matchToggle = matchToggle;
            this.matchAllId = matchAllId;
            this.matchAnyId = matchAnyId;
            this.emptyView = emptyView;
            this.container = container;
            matchToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> schedulePreview());
            updateVisibility();
        }

        void show(@NonNull List<EpisodeCondition> conditions, boolean matchAll) {
            rows.clear();
            container.removeAllViews();
            for (EpisodeCondition condition : conditions) {
                addRow(condition, false);
            }
            matchToggle.check(matchAll ? matchAllId : matchAnyId);
            updateVisibility();
        }

        void addRow(@Nullable EpisodeCondition condition, boolean focus) {
            ConditionRow row = new ConditionRow(container, condition);
            row.removeButton.setOnClickListener(v -> {
                rows.remove(row);
                container.removeView(row.view);
                updateVisibility();
                schedulePreview();
            });
            rows.add(row);
            container.addView(row.view);
            updateVisibility();
            if (focus) {
                row.valueInput.requestFocus();
            }
        }

        boolean isMatchAll() {
            return matchToggle.getCheckedButtonId() == matchAllId;
        }

        @NonNull
        List<EpisodeCondition> conditions() {
            List<EpisodeCondition> conditions = new ArrayList<>();
            for (ConditionRow row : rows) {
                conditions.add(row.condition());
            }
            return conditions;
        }

        private void updateVisibility() {
            emptyView.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
            matchToggle.setVisibility(rows.size() > 1 ? View.VISIBLE : View.GONE);
        }
    }

    private class ConditionRow {
        final View view;
        final Spinner fieldSpinner;
        final Spinner operatorSpinner;
        final TextInputLayout valueLayout;
        final TextInputEditText valueInput;
        final CheckBox wholeWord;
        final View removeButton;
        private EpisodeCondition.Operator[] operators = TEXT_OPERATORS;

        ConditionRow(@NonNull ViewGroup parent, @Nullable EpisodeCondition condition) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.advanced_episode_filter_condition, parent, false);
            fieldSpinner = view.findViewById(R.id.fieldSpinner);
            operatorSpinner = view.findViewById(R.id.operatorSpinner);
            valueLayout = view.findViewById(R.id.valueLayout);
            valueInput = view.findViewById(R.id.valueInput);
            wholeWord = view.findViewById(R.id.wholeWord);
            removeButton = view.findViewById(R.id.removeButton);

            String[] fieldLabels = new String[FIELDS.length];
            for (int i = 0; i < FIELDS.length; i++) {
                fieldLabels[i] = getString(AdvancedEpisodeFilterSettings.fieldLabel(FIELDS[i]));
            }
            fieldSpinner.setAdapter(spinnerAdapter(fieldLabels));
            EpisodeCondition.Field field = condition != null ? condition.getField() : EpisodeCondition.Field.POST;
            fieldSpinner.setSelection(field.ordinal(), false);
            setOperators(field, condition != null ? condition.getOperator() : null);
            if (condition != null) {
                valueInput.setText(condition.getValue());
                wholeWord.setChecked(condition.isWholeWord());
            }
            updateInput();

            fieldSpinner.setOnItemSelectedListener(new SimpleSelectionListener(() -> {
                setOperators(FIELDS[fieldSpinner.getSelectedItemPosition()], selectedOperator());
                updateInput();
                schedulePreview();
            }));
            operatorSpinner.setOnItemSelectedListener(new SimpleSelectionListener(() -> {
                updateInput();
                schedulePreview();
            }));
            wholeWord.setOnCheckedChangeListener((button, checked) -> schedulePreview());
            valueInput.addTextChangedListener(new TextWatcher() {
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
            });
        }

        private void setOperators(@NonNull EpisodeCondition.Field field,
                                  @Nullable EpisodeCondition.Operator selected) {
            EpisodeCondition.Operator[] newOperators = field == EpisodeCondition.Field.LENGTH
                    ? LENGTH_OPERATORS : TEXT_OPERATORS;
            boolean changed = newOperators != operators || operatorSpinner.getAdapter() == null;
            operators = newOperators;
            if (changed) {
                String[] labels = new String[operators.length];
                for (int i = 0; i < operators.length; i++) {
                    labels[i] = getString(AdvancedEpisodeFilterSettings.operatorLabel(operators[i]));
                }
                operatorSpinner.setAdapter(spinnerAdapter(labels));
                if (field == EpisodeCondition.Field.LENGTH) {
                    valueInput.setText("");
                }
            }
            int position = 0;
            for (int i = 0; i < operators.length; i++) {
                if (operators[i] == selected) {
                    position = i;
                }
            }
            operatorSpinner.setSelection(position, false);
        }

        @NonNull
        private EpisodeCondition.Operator selectedOperator() {
            int position = operatorSpinner.getSelectedItemPosition();
            return operators[Math.max(0, Math.min(position, operators.length - 1))];
        }

        private void updateInput() {
            boolean length = FIELDS[fieldSpinner.getSelectedItemPosition()] == EpisodeCondition.Field.LENGTH;
            int inputType = length ? InputType.TYPE_CLASS_NUMBER : InputType.TYPE_CLASS_TEXT;
            if (valueInput.getInputType() != inputType) {
                valueInput.setInputType(inputType);
            }
            valueLayout.setHint(length ? R.string.advanced_filter_minutes_hint
                    : R.string.advanced_filter_value_hint);
            wholeWord.setVisibility(length || selectedOperator() == EpisodeCondition.Operator.IS
                    ? View.GONE : View.VISIBLE);
        }

        @NonNull
        EpisodeCondition condition() {
            String value = valueInput.getText() == null ? "" : valueInput.getText().toString();
            return new EpisodeCondition(FIELDS[fieldSpinner.getSelectedItemPosition()], selectedOperator(),
                    value, wholeWord.getVisibility() == View.VISIBLE && wholeWord.isChecked());
        }

        @NonNull
        private ArrayAdapter<String> spinnerAdapter(@NonNull String[] labels) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_item, labels);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            return adapter;
        }
    }

    private static class SimpleSelectionListener implements AdapterView.OnItemSelectedListener {
        private final Runnable onChange;

        SimpleSelectionListener(@NonNull Runnable onChange) {
            this.onChange = onChange;
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            onChange.run();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }
}
