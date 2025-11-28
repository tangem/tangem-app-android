package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.TangemPayState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotification.Warning.TangemPayRefreshNeeded
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletScreenState

internal class TangemPayRefreshNeededStateTransformer(
    private val onRefreshClick: () -> Unit,
) : WalletScreenStateTransformer {

    override fun transform(prevState: WalletScreenState): WalletScreenState {
        val tangemPayState = TangemPayState.RefreshNeeded(
            notification = TangemPayRefreshNeeded(
                tangemIcon = R.drawable.ic_tangem_24,
                onRefreshClick = onRefreshClick,
            ),
        )
        return prevState.copy(tangemPayState = tangemPayState)
    }
}