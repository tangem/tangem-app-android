package com.tangem.feature.wallet.presentation.wallet.state2.transformers

import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.domain.txhistory.models.TxHistoryListError
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state2.WalletState
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntentsV2
import timber.log.Timber

internal class SetTxHistoryItemsErrorTransformer(
    userWalletId: UserWalletId,
    private val error: TxHistoryListError,
    private val clickIntents: WalletClickIntentsV2,
) : WalletStateTransformer(userWalletId) {

    override fun transform(prevState: WalletState): WalletState {
        return when (prevState) {
            is WalletState.SingleCurrency.Content -> {
                prevState.copy(
                    txHistoryState = when (error) {
                        is TxHistoryListError.DataError -> {
                            TxHistoryState.Error(
                                onReloadClick = clickIntents::onReloadClick,
                                onExploreClick = clickIntents::onExploreClick,
                            )
                        }
                    },
                )
            }
            is WalletState.SingleCurrency.Locked,
            -> {
                Timber.e("Impossible to load transactions history for locked wallet")
                prevState
            }
            is WalletState.MultiCurrency -> {
                Timber.e("Impossible to load transactions history for multi-currency wallet")
                prevState
            }
        }
    }
}