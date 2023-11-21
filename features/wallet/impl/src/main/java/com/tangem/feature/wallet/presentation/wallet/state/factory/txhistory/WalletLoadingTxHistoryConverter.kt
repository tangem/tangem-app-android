package com.tangem.feature.wallet.presentation.wallet.state.factory.txhistory

import androidx.paging.PagingData
import arrow.core.Either
import com.tangem.common.Provider
import com.tangem.core.ui.components.transactions.state.TransactionState
import com.tangem.core.ui.components.transactions.state.TxHistoryState.*
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.txhistory.models.TxHistoryStateError
import com.tangem.feature.wallet.presentation.wallet.state.WalletSingleCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletState
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletClickIntents
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Converter from loading tx history state to [WalletSingleCurrencyState.Content]
 *
 * @property currentStateProvider                    current state provider
 * @property clickIntents                            screen click intents
 *
[REDACTED_AUTHOR]
 */
internal class WalletLoadingTxHistoryConverter(
    private val currentStateProvider: Provider<WalletState>,
    private val currentCardTypeResolverProvider: Provider<CardTypesResolver>,
    private val clickIntents: WalletClickIntents,
) : Converter<WalletLoadingTxHistoryConverter.WalletLoadingTxHistoryModel, WalletState> {

    private val txHistoryItemConverter by lazy {
        val blockchain = currentCardTypeResolverProvider().getBlockchain()
        WalletPendingTxToTransactionStateConverter(
            symbol = blockchain.currency,
            decimals = blockchain.decimals(),
            clickIntents = clickIntents,
        )
    }

    override fun convert(value: WalletLoadingTxHistoryModel): WalletState {
        return value.historyLoadingState.fold(
            ifLeft = { convertError(it, value.pendingTransactions) },
            ifRight = ::convertRight,
        )
    }

    private fun convertError(error: TxHistoryStateError, pendingTransactions: Set<TxHistoryItem>): WalletState {
        val state = currentStateProvider()

        return if (state is WalletSingleCurrencyState.Content) {
            state.copy(
                txHistoryState = when (error) {
                    is TxHistoryStateError.EmptyTxHistories -> Empty(onExploreClick = clickIntents::onExploreClick)
                    is TxHistoryStateError.DataError -> Error(
                        onReloadClick = clickIntents::onReloadClick,
                        onExploreClick = clickIntents::onExploreClick,
                    )
                    is TxHistoryStateError.TxHistoryNotImplemented -> {
                        NotSupported(
                            pendingTransactions = txHistoryItemConverter.convertList(pendingTransactions)
                                .toImmutableList(),
                            onExploreClick = clickIntents::onExploreClick,
                        )
                    }
                },
            )
        } else {
            state
        }
    }

    private fun convertRight(value: Int): WalletState {
        val state = currentStateProvider()
        val singleCurrencyContentState = state as? WalletSingleCurrencyState.Content ?: return state
        return if (singleCurrencyContentState.txHistoryState is Content) {
            singleCurrencyContentState.txHistoryState.contentItems.update {
                PagingData.from(data = createLoadingItems(value))
            }
            state
        } else {
            val txHistoryContent = Content(
                contentItems = MutableStateFlow(
                    value = PagingData.from(data = createLoadingItems(value)),
                ),
            )
            state.copy(txHistoryState = txHistoryContent)
        }
    }

    private fun createLoadingItems(size: Int): List<TxHistoryItemState> {
        return buildList {
            add(TxHistoryItemState.Title(onExploreClick = clickIntents::onExploreClick))
            (1..size).forEach {
                add(TxHistoryItemState.Transaction(state = TransactionState.Loading(it.toString())))
            }
        }
    }

    data class WalletLoadingTxHistoryModel(
        val historyLoadingState: Either<TxHistoryStateError, Int>,
        val pendingTransactions: Set<TxHistoryItem>,
    )
}