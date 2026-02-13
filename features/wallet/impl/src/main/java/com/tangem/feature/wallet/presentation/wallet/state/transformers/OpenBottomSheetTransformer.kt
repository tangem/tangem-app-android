package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletUM

internal class OpenBottomSheetTransformer(
    userWalletId: UserWalletId,
    private val content: TangemBottomSheetConfigContent,
    private val onDismissBottomSheet: () -> Unit,
) : WalletStateTransformer(userWalletId) {

    override fun transform(prevState: WalletState): WalletState {
        return when (prevState) {
            is WalletState.MultiCurrency.Content -> prevState.copy(
                bottomSheetConfig = updateConfig(),
            )
            is WalletState.MultiCurrency.Locked -> prevState.copy(
                bottomSheetConfig = updateConfig(),
            )
            is WalletState.SingleCurrency.Content -> prevState.copy(
                bottomSheetConfig = updateConfig(),
            )
            is WalletState.SingleCurrency.Locked -> prevState.copy(
                bottomSheetConfig = updateConfig(),
            )
        }
    }

    override fun transform(walletUM: WalletUM): WalletUM {
        return walletUM // todo redesign main
    }

    private fun updateConfig() = TangemBottomSheetConfig(
        isShown = true,
        onDismissRequest = onDismissBottomSheet,
        content = content,
    )
}