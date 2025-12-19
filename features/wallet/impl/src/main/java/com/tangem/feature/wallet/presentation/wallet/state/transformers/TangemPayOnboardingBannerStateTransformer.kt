package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.model.TangemPayState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState

internal class TangemPayOnboardingBannerStateTransformer(
    userWalletId: UserWalletId,
    private val onClick: (UserWalletId) -> Unit,
    private val closeOnClick: (UserWalletId) -> Unit,
) : WalletStateTransformer(userWalletId) {

    override fun transform(prevState: WalletState): WalletState {
        return if (prevState is WalletState.MultiCurrency.Content) {
            prevState.copy(
                tangemPayState = TangemPayState.OnboardingBanner(
                    onClick = { onClick(userWalletId) },
                    closeOnClick = { closeOnClick(userWalletId) },
                ),
            )
        } else {
            prevState
        }
    }
}