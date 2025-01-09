package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState

internal class CloseBottomSheetTransformer(userWalletId: UserWalletId) : WalletStateTransformer(userWalletId) {

    override fun transform(prevState: WalletState): WalletState {
        return when (prevState) {
            is WalletState.MultiCurrency.Content -> prevState.copy(
                bottomSheetConfig = updateConfig(prevState),
            )
            is WalletState.MultiCurrency.Locked -> prevState.copy(
                bottomSheetConfig = updateConfig(prevState),
            )
            is WalletState.SingleCurrency.Content -> prevState.copy(
                bottomSheetConfig = updateConfig(prevState),
            )
            is WalletState.SingleCurrency.Locked -> prevState.copy(
                bottomSheetConfig = updateConfig(prevState),
            )
            is WalletState.Visa.Content -> prevState.copy(
                bottomSheetConfig = updateConfig(prevState),
            )
            is WalletState.Visa.Locked -> prevState.copy(
                bottomSheetConfig = updateConfig(prevState),
            )
        }
    }

    private fun updateConfig(prevState: WalletState) = prevState.bottomSheetConfig?.copy(
        isShown = false,
    )
}