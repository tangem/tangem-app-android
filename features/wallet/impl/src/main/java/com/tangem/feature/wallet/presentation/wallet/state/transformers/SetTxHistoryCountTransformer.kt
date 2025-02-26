package com.tangem.feature.wallet.presentation.wallet.state.transformers

import androidx.paging.PagingData
import com.tangem.core.ui.components.transactions.state.TransactionState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

internal class SetTxHistoryCountTransformer(
    userWalletId: UserWalletId,
    private val transactionsCount: Int,
    private val clickIntents: WalletClickIntents,
) : WalletStateTransformer(userWalletId) {

    override fun transform(prevState: WalletState): WalletState {
        return when (prevState) {
            is WalletState.SingleCurrency.Content -> prevState.copy(
                txHistoryState = prevState.txHistoryState.toLoadingState(),
            )
            is WalletState.Visa.Content -> prevState.copy(
                txHistoryState = prevState.txHistoryState.toLoadingState(),
            )
            is WalletState.SingleCurrency.Locked,
            is WalletState.Visa.Locked,
            is WalletState.Visa.AccessTokenLocked,
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

    private fun TxHistoryState.toLoadingState(): TxHistoryState {
        return if (this is TxHistoryState.Content) {
            Timber.d("Load transactions history: $transactionsCount")

            copy(
                contentItems = contentItems.apply {
                    update {
                        PagingData.from(data = createLoadingItems())
                    }
                },
            )
        } else {
            TxHistoryState.Content(
                contentItems = MutableStateFlow(PagingData.from(createLoadingItems())),
            )
        }
    }

    private fun createLoadingItems(): List<TxHistoryState.TxHistoryItemState> {
        return buildList {
            add(TxHistoryState.TxHistoryItemState.Title(onExploreClick = clickIntents::onExploreClick))
            (1..transactionsCount).forEach {
                add(TxHistoryState.TxHistoryItemState.Transaction(state = TransactionState.Loading(it.toString())))
            }
        }
    }
}