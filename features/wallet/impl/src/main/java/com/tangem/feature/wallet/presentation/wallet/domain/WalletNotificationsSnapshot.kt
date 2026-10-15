package com.tangem.feature.wallet.presentation.wallet.domain

import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotificationUM
import kotlinx.collections.immutable.ImmutableList

/**
 * Notifications together with how far the balance they were built from has loaded. Deciding whether the promo cards
 * come back (TWI-1841) needs both from the same emission: a separately observed balance can run ahead of the cards
 * and let the promo cards flash before a balance-dependent card such as Add funds appears.
 */
internal data class WalletNotificationsSnapshot(
    val notifications: ImmutableList<WalletNotificationUM>,
    val balanceState: NotificationsBalanceState,
)

internal enum class NotificationsBalanceState {

    /** The total balance is still loading, so the balance-dependent cards are not built yet */
    Loading,

    /** Built from a resolved balance, cache included, while a network status is still being refreshed */
    Resolved,

    /** No refresh in flight can change the cards anymore */
    Settled,
}