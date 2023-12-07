package com.tangem.feature.wallet.presentation.wallet.state2.transformers

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state2.WalletState

internal class OpenBottomSheetTransformer(
    userWalletId: UserWalletId,
    private val content: TangemBottomSheetConfigContent,
    private val onDismissBottomSheet: () -> Unit,
) : WalletStateTransformer(userWalletId) {

    override fun transform(prevState: WalletState): WalletState {
        return when (prevState) {
            is WalletState.MultiCurrency.Content -> {
                prevState.copy(
                    bottomSheetConfig = TangemBottomSheetConfig(
                        isShow = true,
                        onDismissRequest = onDismissBottomSheet,
                        content = content,
                    ),
                )
            }
            is WalletState.MultiCurrency.Locked -> {
                prevState.copy(isBottomSheetShow = true, onBottomSheetDismiss = onDismissBottomSheet)
            }
            is WalletState.SingleCurrency.Content -> {
                prevState.copy(
                    bottomSheetConfig = TangemBottomSheetConfig(
                        isShow = true,
                        onDismissRequest = onDismissBottomSheet,
                        content = content,
                    ),
                )
            }
            is WalletState.SingleCurrency.Locked -> {
                prevState.copy(isBottomSheetShow = true, onBottomSheetDismiss = onDismissBottomSheet)
            }
            is WalletState.Visa.Content -> prevState.copy(
                bottomSheetConfig = TangemBottomSheetConfig(
                    isShow = true,
                    onDismissRequest = onDismissBottomSheet,
                    content = content,
                ),
            )
            is WalletState.Visa.Locked -> prevState.copy(
                isBottomSheetShow = true,
                onBottomSheetDismiss = onDismissBottomSheet,
            )
        }
    }
}