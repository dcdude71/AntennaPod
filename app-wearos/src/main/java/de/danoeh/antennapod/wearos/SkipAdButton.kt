package de.danoeh.antennapod.wearos

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import de.danoeh.antennapod.net.sync.wearinterface.CastlistWearAdSkip
import de.danoeh.antennapod.ui.common.R as CommonR
import de.danoeh.antennapod.ui.notifications.R as NotificationsR
import de.danoeh.antennapod.wearos.sync.WearMessageSender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object AdSkipRepository {
    private val _seconds = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val seconds: StateFlow<Map<Long, Int>> = _seconds

    fun handleMessage(path: String, data: ByteArray?): Boolean {
        val itemId = CastlistWearAdSkip.parseAdSkipPath(path) ?: return false
        val value = CastlistWearAdSkip.secondsFromBytes(data)
        _seconds.update { it + (itemId to value) }
        return true
    }
}

@Composable
fun SkipAdButton(itemId: Long) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val secondsByItem by AdSkipRepository.seconds.collectAsState()
    val seconds = secondsByItem[itemId] ?: 0

    LaunchedEffect(itemId) {
        WearMessageSender.send(context, CastlistWearAdSkip.adSkipPath(itemId))
    }

    if (seconds <= 0) {
        return
    }
    FilledTonalButton(
        onClick = { scope.launch { WearMessageSender.send(context, CastlistWearAdSkip.SKIP_AD) } },
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        icon = {
            Icon(
                painter = painterResource(NotificationsR.drawable.ic_notification_skip_ad),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        },
        label = { Text(stringResource(CommonR.string.skip_ad_label, seconds)) }
    )
}
