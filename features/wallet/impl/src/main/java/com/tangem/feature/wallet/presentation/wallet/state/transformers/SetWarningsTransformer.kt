package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotification
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
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
            is WalletState.Visa.Content -> prevState.copy(warnings = warnings)
            is WalletState.MultiCurrency.Locked,
            is WalletState.SingleCurrency.Locked,
            is WalletState.Visa.Locked,
            is WalletState.Visa.AccessTokenLocked,
            -> {
                Timber.w("Impossible to update notifications for locked wallet")
                prevState
            }
        }
    }
}