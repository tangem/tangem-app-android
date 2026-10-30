package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotificationUM
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletUM
import kotlinx.collections.immutable.toImmutableList

internal class LinkWalletShowProgressTransformer(
    userWalletId: UserWalletId,
    val shouldShowProgress: Boolean,
) : WalletStateTransformer(userWalletId) {

    override fun transform(prevState: WalletState): WalletState = prevState

    override fun transform(walletUM: WalletUM): WalletUM {
        val content = walletUM as? WalletUM.Content ?: return walletUM
        val newNotifications = content.notifications.map { notification ->
            if (notification is WalletNotificationUM.LinkWalletToDevice) {
                notification.copy(shouldShowProgress = shouldShowProgress)
            } else {
                notification
            }
        }

        return content.copy(notifications = newNotifications.toImmutableList())
    }
}