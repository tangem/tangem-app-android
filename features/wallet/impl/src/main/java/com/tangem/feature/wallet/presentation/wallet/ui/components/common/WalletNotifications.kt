package com.tangem.feature.wallet.presentation.wallet.ui.components.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.components.notifications.OkxPromoNotification
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotification
import kotlinx.collections.immutable.ImmutableList

/**
 * Wallet notifications
 *
 * @param configs  list of notifications
 * @param modifier modifier
 *
* [REDACTED_AUTHOR]
 */
@OptIn(ExperimentalFoundationApi::class)
internal fun LazyListScope.notifications(configs: ImmutableList<WalletNotification>, modifier: Modifier = Modifier) {
    items(
        items = configs,
        key = { it::class.java },
        contentType = { it::class.java },
        itemContent = {
// [REDACTED_TODO_COMMENT]
            when (it) {
                is WalletNotification.SwapPromo -> {
                    OkxPromoNotification(
                        config = it.config,
                        modifier = modifier.animateItemPlacement(),
                    )
                }
                else -> {
                    Notification(
                        config = it.config,
                        modifier = modifier.animateItemPlacement(),
                        iconTint = when (it) {
                            is WalletNotification.Critical -> TangemTheme.colors.icon.warning
                            is WalletNotification.Informational -> TangemTheme.colors.icon.accent
                            is WalletNotification.RateApp -> TangemTheme.colors.icon.attention
                            is WalletNotification.UnlockWallets -> TangemTheme.colors.icon.primary1
                            else -> null
                        },
                    )
                }
            }
        },
    )
}
