package de.danoeh.antennapod.ui.screen.playlist;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.net.download.serviceinterface.AutoDownloadManager;

public class PlaylistEditFragment extends Fragment {
    public static final String TAG = "PlaylistEditFragment";
    private static final String ARG_PLAYLIST_ID = "playlist_id";

    private View rulesPane;
    private View divider;
    private View previewPane;

    public static PlaylistEditFragment newInstance(long playlistId) {
        PlaylistEditFragment fragment = new PlaylistEditFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_PLAYLIST_ID, playlistId);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.playlist_edit_fragment, container, false);
        rulesPane = root.findViewById(R.id.rulesPane);
        divider = root.findViewById(R.id.editPaneDivider);
        previewPane = root.findViewById(R.id.previewPane);
        long playlistId = requireArguments().getLong(ARG_PLAYLIST_ID);
        FragmentManager fragmentManager = getChildFragmentManager();
        if (fragmentManager.findFragmentById(R.id.rulesPane) == null) {
            fragmentManager.beginTransaction()
                    .replace(R.id.rulesPane, PlaylistRulesFragment.newInstance(playlistId))
                    .replace(R.id.previewPane, PlaylistEpisodesFragment.newPreviewInstance(playlistId))
                    .commit();
        }
        applyPanes();
        return root;
    }

    @Override
    public void onStop() {
        super.onStop();
        AutoDownloadManager.getInstance().autodownloadUndownloadedItems(requireContext().getApplicationContext());
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (getView() == null) {
            return;
        }
        applyPanes();
        Fragment rules = getChildFragmentManager().findFragmentById(R.id.rulesPane);
        if (rules instanceof PlaylistRulesFragment) {
            ((PlaylistRulesFragment) rules).updateSubtitle();
        }
    }

    boolean isPreviewShown() {
        return isAdded() && PlaylistPanes.isWide(requireContext());
    }

    private void applyPanes() {
        PlaylistPanes.layoutPanes(rulesPane, divider, previewPane, PlaylistPanes.isWide(requireContext()));
    }
}
