package de.danoeh.antennapod.ui.screen.preferences;

import android.os.Bundle;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.ui.preferences.screen.AnimatedPreferenceFragment;

public class CastlistPreferencesFragment extends AnimatedPreferenceFragment {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_castlist);
    }

    @Override
    public void onStart() {
        super.onStart();
        ((PreferenceActivity) getActivity()).getSupportActionBar().setTitle(R.string.castlist_settings_title);
    }
}
