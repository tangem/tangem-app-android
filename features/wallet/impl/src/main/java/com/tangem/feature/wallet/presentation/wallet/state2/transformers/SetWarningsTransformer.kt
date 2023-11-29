package com.tangem.feature.wallet.presentation.wallet.state2.transformers

import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletNotification
import com.tangem.feature.wallet.presentation.wallet.state2.WalletState
import kotlinx.collections.immutable.ImmutableList
import timber.log.Timber

internal class SetWarningsTransformer(
    userWalletId: UserWalletId,
    private val warnings: ImmutableList<WalletNotification>,
) : WalletStateTransformer(userWalletId) {

    override fun transform(prevState: WalletState): WalletState {
        return when (prevState) {
            is WalletState.MultiCurrency.Content -> prevState.copy(warnings = warnings)
            is WalletState.SingleCurrency.Content -> prevState.copy(warnings = warnings)
            is WalletState.MultiCurrency.Locked,
            is WalletState.SingleCurrency.Locked,
            -> {
                Timber.e("Impossible to update notifications for locked wallet")
                prevState
            }
        }
    }
}
