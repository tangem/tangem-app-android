package com.tangem.feature.wallet.presentation.wallet.state2.transformers

import androidx.paging.PagingData
import com.tangem.core.ui.components.transactions.state.TransactionState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state2.WalletState
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntentsV2
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

internal class SetTxHistoryCountTransformer(
    userWalletId: UserWalletId,
    private val transactionsCount: Int,
    private val clickIntents: WalletClickIntentsV2,
) : WalletStateTransformer(userWalletId) {

    override fun transform(prevState: WalletState): WalletState {
        return when (prevState) {
            is WalletState.SingleCurrency.Content -> prevState.toLoadingState()
            is WalletState.SingleCurrency.Locked,
            -> {
                Timber.e("Impossible to load transactions history for locked wallet")
                prevState
            }
            is WalletState.MultiCurrency,
            -> {
                Timber.e("Impossible to load transactions history for multi-currency wallet")
                prevState
            }
        }
    }

    private fun WalletState.SingleCurrency.Content.toLoadingState(): WalletState {
        return if (txHistoryState is TxHistoryState.Content) {
            (txHistoryState as? TxHistoryState.Content)?.contentItems?.update {
                Timber.d("Load transactions history: $transactionsCount")
                PagingData.from(data = createLoadingItems())
            }
            this
        } else {
            val txHistoryContent = TxHistoryState.Content(
                contentItems = MutableStateFlow(
                    value = PagingData.from(data = createLoadingItems()),
                ),
            )
            copy(txHistoryState = txHistoryContent)
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