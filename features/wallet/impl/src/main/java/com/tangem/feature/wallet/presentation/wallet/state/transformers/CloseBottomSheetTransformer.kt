package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletUM

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
        }
    }

    override fun transform(walletUM: WalletUM): WalletUM {
        return walletUM // todo redesign main
    }

    private fun updateConfig(prevState: WalletState) = prevState.bottomSheetConfig?.copy(
        isShown = false,
    )
}