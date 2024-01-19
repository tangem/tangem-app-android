package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.txhistory

import androidx.paging.PagingData
import arrow.core.Either
import com.tangem.core.ui.components.transactions.state.TransactionState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.domain.txhistory.models.TxHistoryStateError
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.txhistory.TokenDetailsLoadingTxHistoryConverter.TokenDetailsLoadingTxHistoryModel
import com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels.TokenDetailsClickIntents
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class TokenDetailsLoadingTxHistoryConverter(
    private val currentStateProvider: Provider<TokenDetailsState>,
    private val clickIntents: TokenDetailsClickIntents,
) : Converter<TokenDetailsLoadingTxHistoryModel, TokenDetailsState> {

    override fun convert(value: TokenDetailsLoadingTxHistoryModel): TokenDetailsState {
        return value.historyLoadingState.fold(
            ifLeft = { convertError(error = it, pendingTransactions = value.pendingTransactions) },
            ifRight = ::convert,
        )
    }

    private fun convertError(
        error: TxHistoryStateError,
        pendingTransactions: List<TransactionState>,
    ): TokenDetailsState {
        return currentStateProvider().copy(
            txHistoryState = when (error) {
                is TxHistoryStateError.EmptyTxHistories -> TxHistoryState.Empty(clickIntents::onExploreClick)
                is TxHistoryStateError.DataError -> TxHistoryState.Error(
                    onReloadClick = clickIntents::onReloadClick,
                    onExploreClick = clickIntents::onExploreClick,
                )
                is TxHistoryStateError.TxHistoryNotImplemented -> {
                    TxHistoryState.NotSupported(
                        pendingTransactions = pendingTransactions.toImmutableList(),
                        onExploreClick = clickIntents::onExploreClick,
                    )
                }
            },
        )
    }

    private fun convert(value: Int): TokenDetailsState {
        val state = currentStateProvider()

        return if (state.txHistoryState is TxHistoryState.Content) {
            state.txHistoryState.contentItems.update {
                PagingData.from(data = createLoadingItems(value))
            }
            state
        } else {
            val txHistoryContent = TxHistoryState.Content(
                contentItems = MutableStateFlow(
                    value = PagingData.from(data = createLoadingItems(value)),
                ),
            )
            state.copy(txHistoryState = txHistoryContent)
        }
    }

    private fun createLoadingItems(size: Int): List<TxHistoryState.TxHistoryItemState> {
        return buildList {
            add(TxHistoryState.TxHistoryItemState.Title(onExploreClick = clickIntents::onExploreClick))
            (1..size).forEach {
                add(TxHistoryState.TxHistoryItemState.Transaction(state = TransactionState.Loading(it.toString())))
            }
        }
    }

    data class TokenDetailsLoadingTxHistoryModel(
        val historyLoadingState: Either<TxHistoryStateError, Int>,
        val pendingTransactions: List<TransactionState>,
    )
}