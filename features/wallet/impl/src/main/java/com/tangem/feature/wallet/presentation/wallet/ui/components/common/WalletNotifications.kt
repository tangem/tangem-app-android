package com.tangem.feature.wallet.presentation.wallet.ui.components.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletNotification
import kotlinx.collections.immutable.ImmutableList

/**
 * Wallet notifications
 *
 * @param configs  list of notifications
 * @param modifier modifier
 *
[REDACTED_AUTHOR]
 */
@OptIn(ExperimentalFoundationApi::class)
internal fun LazyListScope.notifications(configs: ImmutableList<WalletNotification>, modifier: Modifier = Modifier) {
    items(
        items = configs,
        key = { it.state.title.hashCode() },
        contentType = { it.state::class.java },
        itemContent = { Notification(state = it.state, modifier = modifier.animateItemPlacement()) },
    )
}