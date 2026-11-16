package com.tangem.features.feed.components.v2.blocks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.ui.ds2.modal.LocalTangemModalHost
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

/**
 * Hands a [pending] value to [onClose] once no modal is on screen. A dismissed modal keeps collapsing for a
 * moment after its slot is gone, so a block that opens a screen from its chooser waits it out: the screen zooms
 * out of the block's card, which the collapsing sheet still covers.
 */
@Composable
internal fun <T : Any> AfterModalsClosed(pending: StateFlow<T?>, onClose: (T) -> Unit) {
    val value by pending.collectAsStateWithLifecycle()
    val modalHost = LocalTangemModalHost.current
    val currentOnClose by rememberUpdatedState(onClose)
    val target = value ?: return

    LaunchedEffect(target, modalHost) {
        if (modalHost != null) {
            snapshotFlow { modalHost.hasVisibleModal }.first { hasVisibleModal -> !hasVisibleModal }
        }
        currentOnClose(target)
    }
}