package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.txhistory

import androidx.paging.PagingData
import arrow.core.Either
import com.tangem.common.Provider
import com.tangem.core.ui.components.transactions.intents.TxHistoryClickIntents
import com.tangem.core.ui.components.transactions.state.TransactionState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.domain.txhistory.models.TxHistoryStateError
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.utils.converter.Converter
import kotlinx.coroutines.flow.update

internal class TokenDetailsLoadingTxHistoryConverter(
    private val currentStateProvider: Provider<TokenDetailsState>,
    private val clickIntents: TxHistoryClickIntents,
) : Converter<Either<TxHistoryStateError, Int>, TokenDetailsState> {

    override fun convert(value: Either<TxHistoryStateError, Int>): TokenDetailsState {
        return value.fold(ifLeft = ::convertError, ifRight = ::convert)
    }

    private fun convertError(error: TxHistoryStateError): TokenDetailsState {
        return currentStateProvider().copy(
            txHistoryState = when (error) {
                is TxHistoryStateError.EmptyTxHistories -> {
                    TxHistoryState.Empty(onBuyClick = clickIntents::onBuyClick)
                }
                is TxHistoryStateError.DataError -> {
                    TxHistoryState.Error(onReloadClick = clickIntents::onReloadClick)
                }
                is TxHistoryStateError.TxHistoryNotImplemented -> {
                    TxHistoryState.NotSupported(onExploreClick = clickIntents::onExploreClick)
                }
            },
        )
    }

    private fun convert(value: Int): TokenDetailsState {
        val state = currentStateProvider()
        val txHistoryContent = state.txHistoryState as TxHistoryState.Content

        txHistoryContent.contentItems.update {
            PagingData.from(
                data = listOf(TxHistoryState.TxHistoryItemState.Title(onExploreClick = clickIntents::onExploreClick)) +
                    MutableList(
                        size = value,
                        init = {
                            TxHistoryState.TxHistoryItemState.Transaction(
                                state = TransactionState.Loading(it.toString()),
                            )
                        },
                    ),
            )
        }

        return state
    }
}