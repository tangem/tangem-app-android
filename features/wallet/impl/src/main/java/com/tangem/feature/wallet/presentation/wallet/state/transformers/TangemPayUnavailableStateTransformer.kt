package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.feature.wallet.presentation.wallet.state.model.TangemPayState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotification
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletScreenState

internal object TangemPayUnavailableStateTransformer : WalletScreenStateTransformer {

    override fun transform(prevState: WalletScreenState): WalletScreenState {
        return prevState.copy(
            tangemPayState = TangemPayState.TemporaryUnavailable(
                notification = WalletNotification.Warning.TangemPayUnreachable,
            ),
        )
    }
}