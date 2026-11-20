package de.danoeh.antennapod.wearos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.danoeh.antennapod.net.sync.wearinterface.CastlistWearPlaylists
import de.danoeh.antennapod.net.sync.wearinterface.WearConnectionUtils
import de.danoeh.antennapod.wearos.sync.WearMessageSender
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

object PlaylistRepository {
    private val _playlists = MutableStateFlow<List<CastlistWearPlaylists.Entry>?>(null)
    val playlists: StateFlow<List<CastlistWearPlaylists.Entry>?> = _playlists

    fun handleMessage(path: String, data: ByteArray?): Boolean {
        if (path != CastlistWearPlaylists.PLAYLISTS) {
            return false
        }
        _playlists.value = CastlistWearPlaylists.fromBytes(data)
        return true
    }
}

data class PlaylistListUiState(
    val playlists: List<CastlistWearPlaylists.Entry>? = null,
    val isPhoneSupported: Boolean = true,
    val isTimedOut: Boolean = false
)

class PlaylistListViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(PlaylistListUiState(playlists = PlaylistRepository.playlists.value))
    val uiState: StateFlow<PlaylistListUiState> = _uiState

    init {
        viewModelScope.launch {
            PlaylistRepository.playlists.collect { playlists ->
                _uiState.update { it.copy(playlists = playlists) }
            }
        }

        viewModelScope.launch {
            val isSupported = withContext(Dispatchers.IO) {
                WearConnectionUtils.isPhoneSupported(getApplication())
            }
            if (!isSupported) {
                _uiState.update { it.copy(isPhoneSupported = false) }
                return@launch
            }
            val hadData = PlaylistRepository.playlists.value != null
            WearMessageSender.send(getApplication(), CastlistWearPlaylists.PLAYLISTS)
            if (hadData) {
                return@launch
            }
            val received = withTimeoutOrNull(10.seconds) {
                PlaylistRepository.playlists.filterNotNull().first()
            }
            if (received == null) {
                _uiState.update { it.copy(isTimedOut = true) }
            }
        }
    }

    fun play(playlistId: Long) {
        viewModelScope.launch {
            WearMessageSender.send(getApplication(), CastlistWearPlaylists.playPlaylistPath(playlistId))
        }
    }
}
