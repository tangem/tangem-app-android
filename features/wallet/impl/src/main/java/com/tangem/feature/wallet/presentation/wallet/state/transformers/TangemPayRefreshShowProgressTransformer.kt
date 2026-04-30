package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotification
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletUM
import kotlinx.collections.immutable.toImmutableList

internal class TangemPayRefreshShowProgressTransformer(
    userWalletId: UserWalletId,
    private val shouldShowProgress: Boolean,
) : WalletStateTransformer(userWalletId) {

    override fun transform(prevState: WalletState): WalletState {
        val multiContentState = prevState as? WalletState.MultiCurrency.Content ?: return prevState
        val newWarnings = prevState.warnings.map { warning ->
            if (warning is WalletNotification.Warning.TangemPayRefreshNeeded) {
                warning.copy(shouldShowProgress = shouldShowProgress)
            } else {
                warning
            }
        }

        return multiContentState.copy(warnings = newWarnings.toImmutableList())
    }

    override fun transform(walletUM: WalletUM): WalletUM {
        return walletUM // todo redesign main
    }
}