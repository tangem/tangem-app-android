package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.model.TangemPayState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotification
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState

internal class TangemPayRefreshShowProgressTransformer(
    userWalletId: UserWalletId,
) : WalletStateTransformer(userWalletId) {

    override fun transform(prevState: WalletState): WalletState {
        val multiContentState = prevState as? WalletState.MultiCurrency.Content ?: return prevState
        val refreshNeededState = multiContentState.tangemPayState as? TangemPayState.RefreshNeeded ?: return prevState
        val refreshNotification =
            refreshNeededState.notification as? WalletNotification.Warning.TangemPayRefreshNeeded ?: return prevState

        return multiContentState.copy(
            tangemPayState = refreshNeededState.copy(
                notification = refreshNotification.copy(shouldShowProgress = true),
            ),
        )
    }
}