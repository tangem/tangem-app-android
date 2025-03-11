package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.domain.txhistory.models.TxHistoryListError
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntents
import timber.log.Timber

internal class SetTxHistoryItemsErrorTransformer(
    userWalletId: UserWalletId,
    private val error: TxHistoryListError,
    private val clickIntents: WalletClickIntents,
) : WalletStateTransformer(userWalletId) {

    override fun transform(prevState: WalletState): WalletState {
        return when (prevState) {
            is WalletState.SingleCurrency.Content -> prevState.copy(txHistoryState = createErrorState())
            is WalletState.Visa.Content -> prevState.copy(txHistoryState = createErrorState())
            is WalletState.SingleCurrency.Locked,
            is WalletState.Visa.Locked,
            -> {
                Timber.w("Impossible to load transactions history for locked wallet")
                prevState
            }
            is WalletState.MultiCurrency -> {
                Timber.w("Impossible to load transactions history for multi-currency wallet")
                prevState
            }
        }
    }

    private fun createErrorState(): TxHistoryState.Error = when (error) {
        is TxHistoryListError.DataError -> {
            TxHistoryState.Error(
                onReloadClick = clickIntents::onReloadClick,
                onExploreClick = clickIntents::onExploreClick,
            )
        }
    }
}