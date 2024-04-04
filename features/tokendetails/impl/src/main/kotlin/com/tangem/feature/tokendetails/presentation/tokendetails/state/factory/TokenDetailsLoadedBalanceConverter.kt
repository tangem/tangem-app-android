package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import arrow.core.Either
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.marketprice.PriceChangeState
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.components.marketprice.utils.PriceChangeConverter
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsBalanceBlockState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsNotification
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.txhistory.TokenDetailsTxHistoryTransactionStateConverter
import com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels.TokenDetailsClickIntents
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

internal class TokenDetailsLoadedBalanceConverter(
    private val currentStateProvider: Provider<TokenDetailsState>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val symbol: String,
    private val decimals: Int,
    private val clickIntents: TokenDetailsClickIntents,
) : Converter<Either<CurrencyStatusError, CryptoCurrencyStatus>, TokenDetailsState> {

    private val txHistoryItemConverter by lazy {
        TokenDetailsTxHistoryTransactionStateConverter(symbol, decimals, clickIntents)
    }

    override fun convert(value: Either<CurrencyStatusError, CryptoCurrencyStatus>): TokenDetailsState {
        return value.fold(ifLeft = { convertError() }, ifRight = ::convert)
    }

    private fun convertError(): TokenDetailsState {
        val state = currentStateProvider()
        return state.copy(
            tokenBalanceBlockState = TokenDetailsBalanceBlockState.Error(state.tokenBalanceBlockState.actionButtons),
            marketPriceBlockState = MarketPriceBlockState.Error(state.marketPriceBlockState.currencySymbol),
            notifications = persistentListOf(TokenDetailsNotification.NetworksUnreachable),
        )
    }

    private fun convert(status: CryptoCurrencyStatus): TokenDetailsState {
        val state = currentStateProvider()
        val currencyName = state.marketPriceBlockState.currencySymbol
        val pendingTxs = status.value.pendingTransactions.map(txHistoryItemConverter::convert).toPersistentList()
        return state.copy(
            tokenBalanceBlockState = getBalanceState(state.tokenBalanceBlockState, status),
            marketPriceBlockState = getMarketPriceState(status = status.value, currencySymbol = currencyName),
            pendingTxs = pendingTxs,
            txHistoryState = if (state.txHistoryState is TxHistoryState.NotSupported) {
                state.txHistoryState.copy(pendingTransactions = pendingTxs)
            } else {
                state.txHistoryState
            },
        )
    }

    private fun getBalanceState(
        currentState: TokenDetailsBalanceBlockState,
        status: CryptoCurrencyStatus,
    ): TokenDetailsBalanceBlockState {
        return when (status.value) {
            is CryptoCurrencyStatus.NoQuote,
            is CryptoCurrencyStatus.Loaded,
            is CryptoCurrencyStatus.NoAccount,
            is CryptoCurrencyStatus.Custom,
            -> TokenDetailsBalanceBlockState.Content(
                actionButtons = currentState.actionButtons,
                fiatBalance = formatFiatAmount(status.value, appCurrencyProvider()),
                cryptoBalance = formatCryptoAmount(status),
            )
            is CryptoCurrencyStatus.Loading -> TokenDetailsBalanceBlockState.Loading(currentState.actionButtons)
            is CryptoCurrencyStatus.MissedDerivation,
            is CryptoCurrencyStatus.Unreachable,
            is CryptoCurrencyStatus.NoAmount,
            -> TokenDetailsBalanceBlockState.Error(currentState.actionButtons)
        }
    }

    private fun getMarketPriceState(
        status: CryptoCurrencyStatus.Value,
        currencySymbol: String,
    ): MarketPriceBlockState {
        return when (status) {
            is CryptoCurrencyStatus.Loading -> MarketPriceBlockState.Loading(currencySymbol)
            is CryptoCurrencyStatus.NoQuote -> MarketPriceBlockState.Error(currencySymbol)
            is CryptoCurrencyStatus.NoAccount -> {
                if (status.fiatRate == null) {
                    MarketPriceBlockState.Error(currencySymbol)
                } else {
                    status.toContentConfig(currencySymbol)
                }
            }
            is CryptoCurrencyStatus.Loaded,
            is CryptoCurrencyStatus.Custom,
            is CryptoCurrencyStatus.MissedDerivation,
            is CryptoCurrencyStatus.Unreachable,
            is CryptoCurrencyStatus.NoAmount,
            -> status.toContentConfig(currencySymbol)
        }
    }

    private fun CryptoCurrencyStatus.Value.toContentConfig(currencySymbol: String): MarketPriceBlockState.Content {
        return MarketPriceBlockState.Content(
            currencySymbol = currencySymbol,
            price = formatPrice(status = this, appCurrency = appCurrencyProvider()),
            priceChangeConfig = PriceChangeState.Content(
                valueInPercent = formatPriceChange(status = this),
                type = getPriceChangeType(status = this),
            ),
        )
    }

    private fun getPriceChangeType(status: CryptoCurrencyStatus.Value): PriceChangeType {
        return PriceChangeConverter.fromBigDecimal(status.priceChange)
    }

    private fun formatPriceChange(status: CryptoCurrencyStatus.Value): String {
        val priceChange = status.priceChange ?: return BigDecimalFormatter.EMPTY_BALANCE_SIGN

        return BigDecimalFormatter.formatPercent(
            percent = priceChange,
            useAbsoluteValue = true,
        )
    }

    private fun formatPrice(status: CryptoCurrencyStatus.Value, appCurrency: AppCurrency): String {
        val fiatRate = status.fiatRate ?: return BigDecimalFormatter.EMPTY_BALANCE_SIGN

        return BigDecimalFormatter.formatFiatAmount(
            fiatAmount = fiatRate,
            fiatCurrencyCode = appCurrency.code,
            fiatCurrencySymbol = appCurrency.symbol,
        )
    }

    private fun formatFiatAmount(status: CryptoCurrencyStatus.Value, appCurrency: AppCurrency): String {
        val fiatAmount = status.fiatAmount ?: return BigDecimalFormatter.EMPTY_BALANCE_SIGN

        return BigDecimalFormatter.formatFiatAmount(
            fiatAmount = fiatAmount,
            fiatCurrencyCode = appCurrency.code,
            fiatCurrencySymbol = appCurrency.symbol,
        )
    }

    private fun formatCryptoAmount(status: CryptoCurrencyStatus): String {
        val amount = status.value.amount ?: return BigDecimalFormatter.EMPTY_BALANCE_SIGN

        return BigDecimalFormatter.formatCryptoAmount(amount, status.currency.symbol, status.currency.decimals)
    }
}