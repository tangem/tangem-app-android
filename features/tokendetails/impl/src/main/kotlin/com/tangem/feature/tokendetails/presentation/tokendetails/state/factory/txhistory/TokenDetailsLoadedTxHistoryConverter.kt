package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.txhistory

import androidx.paging.PagingData
import arrow.core.Either
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.txhistory.models.TxHistoryListError
import com.tangem.feature.tokendetails.presentation.tokendetails.model.TokenDetailsClickIntents
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import kotlinx.coroutines.flow.Flow

internal class TokenDetailsLoadedTxHistoryConverter(
    private val currentStateProvider: Provider<TokenDetailsState>,
    private val clickIntents: TokenDetailsClickIntents,
    symbol: String,
    decimals: Int,
) : Converter<Either<TxHistoryListError, Flow<PagingData<TxInfo>>>, TxHistoryState> {

    private val txHistoryItemFlowConverter by lazy {
        TokenDetailsTxHistoryItemFlowConverter(
            currentStateProvider = currentStateProvider,
            symbol = symbol,
            decimals = decimals,
            clickIntents = clickIntents,
        )
    }

    override fun convert(value: Either<TxHistoryListError, Flow<PagingData<TxInfo>>>): TxHistoryState {
        return value.fold(ifLeft = ::convertError, ifRight = ::convert)
    }

    private fun convertError(error: TxHistoryListError): TxHistoryState {
        return when (error) {
            is TxHistoryListError.DataError -> {
                TxHistoryState.Error(
                    onReloadClick = clickIntents::onReloadClick,
                    onExploreClick = clickIntents::onExploreClick,
                )
            }
        }
    }

    private fun convert(items: Flow<PagingData<TxInfo>>): TxHistoryState {
        return txHistoryItemFlowConverter.convert(value = items)
    }
}