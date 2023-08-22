package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.txhistory

import androidx.paging.PagingData
import arrow.core.Either
import com.tangem.common.Provider
import com.tangem.core.ui.components.transactions.intents.TxHistoryClickIntents
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.txhistory.models.TxHistoryListError
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.utils.converter.Converter
import kotlinx.coroutines.flow.Flow

internal class TokenDetailsLoadedTxHistoryConverter(
    private val currentStateProvider: Provider<TokenDetailsState>,
    private val clickIntents: TxHistoryClickIntents,
    symbol: String,
    decimals: Int,
) : Converter<Either<TxHistoryListError, Flow<PagingData<TxHistoryItem>>>, TokenDetailsState> {

    private val txHistoryItemFlowConverter by lazy {
        TokenDetailsTxHistoryItemFlowConverter(
            currentStateProvider = currentStateProvider,
            symbol = symbol,
            decimals = decimals,
            clickIntents = clickIntents,
        )
    }

    override fun convert(value: Either<TxHistoryListError, Flow<PagingData<TxHistoryItem>>>): TokenDetailsState {
        return value.fold(ifLeft = ::convertError, ifRight = ::convert)
    }

    private fun convertError(error: TxHistoryListError): TokenDetailsState {
        return currentStateProvider().copy(
            txHistoryState = when (error) {
                is TxHistoryListError.DataError -> {
                    TxHistoryState.Error(onReloadClick = clickIntents::onReloadClick)
                }
            },
        )
    }

    private fun convert(items: Flow<PagingData<TxHistoryItem>>): TokenDetailsState {
        return currentStateProvider().copy(
            txHistoryState = txHistoryItemFlowConverter.convert(value = items),
        )
    }
}
