package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import androidx.paging.PagingData
import arrow.core.Either
import com.tangem.common.Provider
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.txhistory.models.TxHistoryListError
import com.tangem.domain.txhistory.models.TxHistoryStateError
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.txhistory.TokenDetailsLoadedTxHistoryConverter
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.txhistory.TokenDetailsLoadingTxHistoryConverter
import com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels.TokenDetailsClickIntents
import kotlinx.coroutines.flow.Flow

internal class TokenDetailsStateFactory(
    private val currentStateProvider: Provider<TokenDetailsState>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val clickIntents: TokenDetailsClickIntents,
    symbol: String,
    decimals: Int,
) {

    private val skeletonStateConverter by lazy {
        TokenDetailsSkeletonStateConverter(clickIntents = clickIntents)
    }

    private val tokenDetailsLoadedBalanceConverter by lazy {
        TokenDetailsLoadedBalanceConverter(
            currentStateProvider = currentStateProvider,
            appCurrencyProvider = appCurrencyProvider,
        )
    }

    private val tokenDetailsButtonsConverter by lazy {
        TokenDetailsActionButtonsConverter(
            currentStateProvider = currentStateProvider,
            clickIntents = clickIntents,
        )
    }

    private val loadingTransactionsStateConverter by lazy {
        TokenDetailsLoadingTxHistoryConverter(currentStateProvider = currentStateProvider, clickIntents = clickIntents)
    }

    private val loadedTxHistoryConverter by lazy {
        TokenDetailsLoadedTxHistoryConverter(
            currentStateProvider = currentStateProvider,
            clickIntents = clickIntents,
            symbol = symbol,
            decimals = decimals,
        )
    }

    fun getInitialState(cryptoCurrency: CryptoCurrency): TokenDetailsState {
        return skeletonStateConverter.convert(
            TokenDetailsSkeletonStateConverter.SkeletonModel(cryptoCurrency = cryptoCurrency),
        )
    }

    fun getCurrencyLoadedBalanceState(
        cryptoCurrencyEither: Either<CurrencyStatusError, CryptoCurrencyStatus>,
    ): TokenDetailsState {
        return tokenDetailsLoadedBalanceConverter.convert(cryptoCurrencyEither)
    }

    fun getManageButtonsState(actions: List<TokenActionsState.ActionState>): TokenDetailsState {
        return tokenDetailsButtonsConverter.convert(actions)
    }

    fun getLoadingTxHistoryState(itemsCountEither: Either<TxHistoryStateError, Int>): TokenDetailsState {
        return loadingTransactionsStateConverter.convert(value = itemsCountEither)
    }

    fun getLoadedTxHistoryState(
        txHistoryEither: Either<TxHistoryListError, Flow<PagingData<TxHistoryItem>>>,
    ): TokenDetailsState {
        return loadedTxHistoryConverter.convert(txHistoryEither)
    }
}