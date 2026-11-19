package de.danoeh.antennapod.ui.screen.preferences;

import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.storage.preferences.EpisodeFilterPreferences;
import de.danoeh.antennapod.ui.preferences.screen.AnimatedPreferenceFragment;

public class CastlistPreferencesFragment extends AnimatedPreferenceFragment {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_castlist);
        Preference filterStyle = findPreference(EpisodeFilterPreferences.PREF_EPISODE_FILTER_STYLE);
        if (filterStyle != null) {
            filterStyle.setOnPreferenceChangeListener((preference, newValue) -> {
                if (!newValue.equals(((ListPreference) preference).getValue())) {
                    boolean advanced = EpisodeFilterPreferences.STYLE_ADVANCED.equals(newValue);
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.pref_episode_filter_style_title)
                            .setMessage(advanced ? R.string.episode_filter_style_switched_advanced
                                    : R.string.episode_filter_style_switched_basic)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
                return true;
            });
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        ((PreferenceActivity) getActivity()).getSupportActionBar().setTitle(R.string.castlist_settings_title);
    }
}
