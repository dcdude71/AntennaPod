package de.danoeh.antennapod.ui.screen.feed.preferences;

import android.content.Context;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.storage.database.FeedAdSkip;
import de.danoeh.antennapod.ui.common.databinding.EditTextDialogBinding;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;

final class FeedAdSkipDialog {
    private static final String TAG = "FeedAdSkipDialog";
    private static final int MAX_SECONDS = 3600;

    interface Callback {
        void onSecondsChanged(int seconds);
    }

    private FeedAdSkipDialog() {
    }

    static void show(Context context, long feedId, int currentSeconds, Callback callback) {
        EditTextDialogBinding binding = EditTextDialogBinding.inflate(LayoutInflater.from(context));
        binding.textInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        binding.textInputLayout.setSuffixText(context.getString(R.string.time_seconds));
        binding.textInput.setText(String.valueOf(currentSeconds));
        binding.textInput.selectAll();
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.feed_ad_skip)
                .setMessage(R.string.feed_ad_skip_message)
                .setView(binding.getRoot())
                .setPositiveButton(R.string.confirm_label, (dialog, which) -> {
                    int seconds;
                    try {
                        seconds = Integer.parseInt(String.valueOf(binding.textInput.getText()));
                    } catch (NumberFormatException e) {
                        seconds = 0;
                    }
                    final int newSeconds = Math.max(0, Math.min(MAX_SECONDS, seconds));
                    Completable.fromAction(() -> FeedAdSkip.setSeconds(feedId, newSeconds))
                            .subscribeOn(Schedulers.io())
                            .subscribe(() -> { }, error -> Log.e(TAG, Log.getStackTraceString(error)));
                    callback.onSecondsChanged(newSeconds);
                })
                .setNegativeButton(R.string.cancel_label, null)
                .show();
    }
}
