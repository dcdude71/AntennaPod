package de.danoeh.antennapod.ui.screen.playlist;

import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import de.danoeh.antennapod.R;

final class PlaylistKindDialog {

    interface Callback {
        void onCreate(@NonNull String name, boolean manual);
    }

    private PlaylistKindDialog() {
    }

    static void show(@NonNull Context context, @NonNull Callback callback) {
        View view = LayoutInflater.from(context).inflate(R.layout.playlist_kind_dialog, null);
        MaterialCardView smartCard = view.findViewById(R.id.smartCard);
        final MaterialCardView manualCard = view.findViewById(R.id.manualCard);
        PlaylistKindUi.bindTile((ImageView) view.findViewById(R.id.smartIcon), false);
        PlaylistKindUi.bindTile((ImageView) view.findViewById(R.id.manualIcon), true);
        final EditText nameInput = view.findViewById(R.id.nameInput);
        smartCard.setChecked(true);
        smartCard.setOnClickListener(v -> {
            smartCard.setChecked(true);
            manualCard.setChecked(false);
        });
        manualCard.setOnClickListener(v -> {
            manualCard.setChecked(true);
            smartCard.setChecked(false);
        });
        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.playlist_new)
                .setView(view)
                .setPositiveButton(R.string.playlist_create, (d, which) -> {
                    String name = nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
                    if (!name.isEmpty()) {
                        callback.onCreate(name, manualCard.isChecked());
                    }
                })
                .setNegativeButton(R.string.cancel_label, null)
                .show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
        nameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(s.toString().trim().length() > 0);
            }
        });
    }
}
