package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotification
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotificationUM
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.tangem.utils.logging.TangemLogger

internal class SetWarningsTransformer(
    userWalletId: UserWalletId,
    private val warnings: ImmutableList<WalletNotification>,
    private val notifications: ImmutableList<WalletNotificationUM> = persistentListOf(),
    private val notificationsCarousel: ImmutableList<WalletNotificationUM> = persistentListOf(),
) : WalletStateTransformer(userWalletId) {

    override fun transform(prevState: WalletState): WalletState {
        return when (prevState) {
            is WalletState.MultiCurrency.Content -> prevState.copy(warnings = warnings)
            is WalletState.SingleCurrency.Content -> prevState.copy(warnings = warnings)
            is WalletState.MultiCurrency.Locked,
            is WalletState.SingleCurrency.Locked,
            -> {
                TangemLogger.w("Impossible to update notifications for locked wallet")
                prevState
            }
        }
    }

    override fun transform(walletUM: WalletUM): WalletUM {
        return when (walletUM) {
            is WalletUM.Content -> walletUM.copy(
                notifications = notifications,
                notificationsCarousel = notificationsCarousel,
            )
            is WalletUM.Locked -> {
                TangemLogger.w("Impossible to update notifications for locked wallet")
                walletUM
            }
        }
    }
}