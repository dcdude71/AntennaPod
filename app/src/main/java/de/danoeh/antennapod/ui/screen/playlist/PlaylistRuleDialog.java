package de.danoeh.antennapod.ui.screen.playlist;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.databinding.PlaylistRuleDialogBinding;
import de.danoeh.antennapod.model.playlist.PlaylistRule;

final class PlaylistRuleDialog {
    private static final int DEFAULT_LIMIT = 3;

    interface Callback {
        void onSave(@NonNull PlaylistRule rule);
    }

    private PlaylistRuleDialog() {
    }

    static void show(Context context, String title, PlaylistRule rule, boolean autoDownload, Callback callback) {
        PlaylistRuleDialogBinding binding = PlaylistRuleDialogBinding.inflate(LayoutInflater.from(context));

        boolean all = rule.getEpisodeCount() == PlaylistRule.ALL_EPISODES && !autoDownload;
        int maxLimit = autoDownload ? PlaylistRule.MAX_AUTO_DOWNLOAD_EPISODES : Integer.MAX_VALUE;
        if (autoDownload) {
            binding.countAll.setEnabled(false);
            binding.autoDownloadHint.setVisibility(View.VISIBLE);
            binding.autoDownloadHint.setText(context.getString(R.string.playlist_rule_auto_download_limit,
                    PlaylistRule.MAX_AUTO_DOWNLOAD_EPISODES));
        }
        binding.countGroup.check(all ? R.id.countAll : R.id.countLimit);
        binding.countInput.setText(String.valueOf(all || rule.getEpisodeCount() == PlaylistRule.ALL_EPISODES
                ? DEFAULT_LIMIT : Math.min(rule.getEpisodeCount(), maxLimit)));
        setLimitEnabled(binding, !all);
        binding.countGroup.setOnCheckedChangeListener((group, checkedId) ->
                setLimitEnabled(binding, checkedId == R.id.countLimit));

        binding.pickFromGroup.check(rule.getPickFrom() == PlaylistRule.PickFrom.OLDEST
                ? R.id.pickOldest : R.id.pickMostRecent);
        binding.playOrderGroup.check(rule.getPlayOrder() == PlaylistRule.PlayOrder.NEWEST_FIRST
                ? R.id.playNewestFirst : R.id.playOldestFirst);

        binding.prioritySlider.setValueFrom(PlaylistRule.MIN_PRIORITY);
        binding.prioritySlider.setValueTo(PlaylistRule.MAX_PRIORITY);
        binding.prioritySlider.setValue(rule.getPriority());
        binding.priorityLabel.setText(context.getString(R.string.playlist_rule_priority_value, rule.getPriority()));
        binding.prioritySlider.addOnChangeListener((slider, value, fromUser) ->
                binding.priorityLabel.setText(context.getString(R.string.playlist_rule_priority_value,
                        Math.round(value))));

        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setView(binding.getRoot())
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    rule.setEpisodeCount(binding.countLimit.isChecked()
                            ? Math.min(parseLimit(binding.countInput.getText().toString()), maxLimit)
                            : PlaylistRule.ALL_EPISODES);
                    rule.setPickFrom(binding.pickOldest.isChecked()
                            ? PlaylistRule.PickFrom.OLDEST : PlaylistRule.PickFrom.MOST_RECENT);
                    rule.setPlayOrder(binding.playNewestFirst.isChecked()
                            ? PlaylistRule.PlayOrder.NEWEST_FIRST : PlaylistRule.PlayOrder.OLDEST_FIRST);
                    rule.setPriority(Math.round(binding.prioritySlider.getValue()));
                    callback.onSave(rule);
                })
                .setNegativeButton(R.string.cancel_label, null)
                .show();
    }

    static String getSummary(Context context, PlaylistRule rule) {
        String count;
        int episodeCount = rule.getEpisodeCount();
        if (episodeCount == PlaylistRule.ALL_EPISODES) {
            count = context.getString(R.string.playlist_rule_summary_all);
        } else if (rule.getPickFrom() == PlaylistRule.PickFrom.OLDEST) {
            count = context.getResources().getQuantityString(
                    R.plurals.playlist_rule_summary_oldest, episodeCount, episodeCount);
        } else {
            count = context.getResources().getQuantityString(
                    R.plurals.playlist_rule_summary_most_recent, episodeCount, episodeCount);
        }
        String order = context.getString(rule.getPlayOrder() == PlaylistRule.PlayOrder.NEWEST_FIRST
                ? R.string.playlist_rule_newest_first : R.string.playlist_rule_oldest_first);
        String priority = context.getString(R.string.playlist_rule_summary_priority, rule.getPriority());
        return context.getString(R.string.playlist_rule_summary, count, order, priority);
    }

    private static void setLimitEnabled(PlaylistRuleDialogBinding binding, boolean enabled) {
        binding.countInput.setEnabled(enabled);
        binding.pickMostRecent.setEnabled(enabled);
        binding.pickOldest.setEnabled(enabled);
    }

    private static int parseLimit(String text) {
        try {
            return Math.max(1, Integer.parseInt(text.trim()));
        } catch (NumberFormatException e) {
            return DEFAULT_LIMIT;
        }
    }
}
