package com.tangem.feature.wallet.presentation.wallet.state.transformers

import androidx.paging.PagingData
import com.tangem.core.ui.components.transactions.state.TransactionState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletUM
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import com.tangem.utils.logging.TangemLogger

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
            is WalletState.SingleCurrency.Locked,
            -> {
                TangemLogger.w("Impossible to load transactions history for locked wallet")
                prevState
            }
            is WalletState.MultiCurrency -> {
                TangemLogger.w("Impossible to load transactions history for multi-currency wallet")
                prevState
            }
        }
    }

    override fun transform(walletUM: WalletUM): WalletUM {
        return walletUM // todo redesign main
    }

    private fun TxHistoryState.toLoadingState(): TxHistoryState {
        return if (this is TxHistoryState.Content) {
            TangemLogger.d("Load transactions history: $transactionsCount")

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
            for (i in 1..transactionsCount) {
                add(TxHistoryState.TxHistoryItemState.Transaction(state = TransactionState.Loading(i.toString())))
            }
        }
    }
}