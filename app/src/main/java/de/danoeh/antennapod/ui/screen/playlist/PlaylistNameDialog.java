package de.danoeh.antennapod.ui.screen.playlist;

import android.content.Context;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.ui.common.databinding.EditTextDialogBinding;

final class PlaylistNameDialog {

    interface Callback {
        void onNameEntered(@NonNull String name);
    }

    private PlaylistNameDialog() {
    }

    static void show(Context context, @StringRes int title, String initialName, Callback callback) {
        EditTextDialogBinding binding = EditTextDialogBinding.inflate(LayoutInflater.from(context));
        binding.textInput.setText(initialName);
        binding.textInput.selectAll();
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setView(binding.getRoot())
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String name = binding.textInput.getText() == null
                            ? "" : binding.textInput.getText().toString().trim();
                    if (!name.isEmpty()) {
                        callback.onNameEntered(name);
                    }
                })
                .setNegativeButton(R.string.cancel_label, null)
                .show();
    }
}
