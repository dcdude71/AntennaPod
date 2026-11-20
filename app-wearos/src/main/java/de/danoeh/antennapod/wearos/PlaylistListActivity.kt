package de.danoeh.antennapod.wearos

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.dynamicColorScheme
import de.danoeh.antennapod.net.sync.wearinterface.CastlistWearPlaylists
import de.danoeh.antennapod.ui.common.PlaylistTime
import de.danoeh.antennapod.ui.common.R as CommonR
import de.danoeh.antennapod.wearos.composable.ListItem
import de.danoeh.antennapod.wearos.composable.ListScaffold

class PlaylistListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel = ViewModelProvider(this)[PlaylistListViewModel::class.java]

        setContent {
            MaterialTheme(colorScheme = dynamicColorScheme(this) ?: MaterialTheme.colorScheme) {
                val uiState by viewModel.uiState.collectAsState()
                PlaylistListScreen(
                    uiState = uiState,
                    onPlay = { playlist ->
                        viewModel.play(playlist.id)
                        Toast.makeText(this, getString(CommonR.string.playlist_now_playing, playlist.name),
                            Toast.LENGTH_SHORT).show()
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun PlaylistListScreen(uiState: PlaylistListUiState, onPlay: (CastlistWearPlaylists.Entry) -> Unit) {
    val scrollState = rememberScalingLazyListState()
    val playlists = uiState.playlists

    ListScaffold(
        titleRes = CommonR.string.playlists_label,
        scrollState = scrollState,
        isPhoneSupported = uiState.isPhoneSupported,
        isTimedOut = uiState.isTimedOut,
        isLoading = playlists == null,
        isEmpty = playlists?.isEmpty() == true
    ) {
        items(playlists!!) { playlist ->
            PlaylistButton(playlist = playlist, onClick = { onPlay(playlist) })
        }
    }
}

@Composable
private fun PlaylistButton(playlist: CastlistWearPlaylists.Entry, onClick: () -> Unit) {
    val context = LocalContext.current
    val count = pluralStringResource(CommonR.plurals.playlist_num_episodes, playlist.episodes, playlist.episodes)
    val summary = if (playlist.timeLeftMillis > 0) {
        stringResource(
            CommonR.string.playlist_summary,
            count,
            stringResource(CommonR.string.playlist_time_left, PlaylistTime.format(context, playlist.timeLeftMillis))
        )
    } else {
        count
    }
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        icon = {
            Icon(
                painter = painterResource(id = kindIcon(playlist.kind)),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        },
        secondaryLabel = {
            Text(text = summary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        label = {
            Text(text = playlist.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    )
}

private fun kindIcon(kind: Int): Int = when (kind) {
    CastlistWearPlaylists.KIND_MANUAL -> CommonR.drawable.ic_playlist_kind_manual
    CastlistWearPlaylists.KIND_SMART -> CommonR.drawable.ic_playlist_kind_smart
    else -> CommonR.drawable.ic_playlist_play_black
}

@Composable
fun PlaylistsMenuItem() {
    val context = LocalContext.current
    ListItem(
        text = stringResource(CommonR.string.playlists_label),
        iconRes = CommonR.drawable.ic_playlist_kind_smart,
        onClick = { context.startActivity(Intent(context, PlaylistListActivity::class.java)) }
    )
}
