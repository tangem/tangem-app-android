package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.utils.showSwapBadge

internal class UpdateMultiWalletActionButtonBadgeTransformer(
    private val showSwapBadge: Boolean,
    userWalletId: UserWalletId,
) : WalletStateTransformer(userWalletId) {
    override fun transform(prevState: WalletState): WalletState {
        return when (prevState) {
            is WalletState.MultiCurrency.Content -> {
                prevState.copy(buttons = prevState.showSwapBadge(showBadge = showSwapBadge))
            }
            else -> prevState
        }
    }
}