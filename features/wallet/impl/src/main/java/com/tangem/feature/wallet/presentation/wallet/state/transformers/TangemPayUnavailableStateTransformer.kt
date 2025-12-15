package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.model.TangemPayState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotification
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState

internal class TangemPayUnavailableStateTransformer(
    userWalletId: UserWalletId,
) : WalletStateTransformer(userWalletId) {

    override fun transform(prevState: WalletState): WalletState {
        return if (prevState is WalletState.MultiCurrency.Content) {
            prevState.copy(
                tangemPayState = TangemPayState.TemporaryUnavailable(
                    notification = WalletNotification.Warning.TangemPayUnreachable,
                ),
            )
        } else {
            prevState
        }
    }
}