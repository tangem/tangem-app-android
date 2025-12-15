package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.TangemPayState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotification.Warning.TangemPayRefreshNeeded
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState

internal class TangemPayRefreshNeededStateTransformer(
    userWalletId: UserWalletId,
    private val onRefreshClick: () -> Unit,
) : WalletStateTransformer(userWalletId = userWalletId) {

    override fun transform(prevState: WalletState): WalletState {
        val tangemPayState = TangemPayState.RefreshNeeded(
            notification = TangemPayRefreshNeeded(
                tangemIcon = R.drawable.ic_tangem_24,
                onRefreshClick = onRefreshClick,
            ),
        )
        return if (prevState is WalletState.MultiCurrency.Content) {
            prevState.copy(tangemPayState = tangemPayState)
        } else {
            prevState
        }
    }
}