package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import arrow.core.Either
import com.tangem.common.Provider
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.marketprice.PriceChangeState
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsBalanceBlockState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsNotification
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.txhistory.TokenDetailsTxHistoryToTransactionStateConverter
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal

internal class TokenDetailsLoadedBalanceConverter(
    private val currentStateProvider: Provider<TokenDetailsState>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val isBalanceHiddenProvider: Provider<Boolean>,
    private val symbol: String,
    private val decimals: Int,
) : Converter<Either<CurrencyStatusError, CryptoCurrencyStatus>, TokenDetailsState> {

    private val txHistoryItemConverter by lazy {
        TokenDetailsTxHistoryToTransactionStateConverter(symbol, decimals)
    }

    override fun convert(value: Either<CurrencyStatusError, CryptoCurrencyStatus>): TokenDetailsState {
        return value.fold(ifLeft = { convertError() }, ifRight = ::convert)
    }

    private fun convertError(): TokenDetailsState {
        val state = currentStateProvider()
        return state.copy(
            tokenBalanceBlockState = TokenDetailsBalanceBlockState.Error(state.tokenBalanceBlockState.actionButtons),
            marketPriceBlockState = MarketPriceBlockState.Error(state.marketPriceBlockState.currencyName),
            notifications = persistentListOf(TokenDetailsNotification.NetworksUnreachable),
        )
    }

    private fun convert(status: CryptoCurrencyStatus): TokenDetailsState {
        val state = currentStateProvider()
        val currencyName = state.marketPriceBlockState.currencyName
        return state.copy(
            tokenBalanceBlockState = getBalanceState(state.tokenBalanceBlockState, status),
            marketPriceBlockState = getMarketPriceState(status = status.value, currencyName = currencyName),
            pendingTxs = status.value.pendingTransactions.map(txHistoryItemConverter::convert).toPersistentList(),
        )
    }

    private fun getBalanceState(
        currentState: TokenDetailsBalanceBlockState,
        status: CryptoCurrencyStatus,
    ): TokenDetailsBalanceBlockState {
        return when (status.value) {
            is CryptoCurrencyStatus.NoQuote,
            is CryptoCurrencyStatus.Loaded,
            -> {
                TokenDetailsBalanceBlockState.Content(
                    actionButtons = currentState.actionButtons,
                    fiatBalance = formatFiatAmount(status.value, appCurrencyProvider()),
                    cryptoBalance = formatCryptoAmount(status),
                    isBalanceHidden = isBalanceHiddenProvider(),
                )
            }
            is CryptoCurrencyStatus.Loading -> {
                TokenDetailsBalanceBlockState.Loading(currentState.actionButtons)
            }
            is CryptoCurrencyStatus.MissedDerivation,
            is CryptoCurrencyStatus.NoAccount,
            is CryptoCurrencyStatus.Custom,
            is CryptoCurrencyStatus.NoAmount,
            is CryptoCurrencyStatus.Unreachable,
            -> {
                TokenDetailsBalanceBlockState.Error(currentState.actionButtons)
            }
        }
    }

    private fun getMarketPriceState(status: CryptoCurrencyStatus.Status, currencyName: String): MarketPriceBlockState {
        return when (status) {
            is CryptoCurrencyStatus.Loading -> MarketPriceBlockState.Loading(currencyName)
            is CryptoCurrencyStatus.NoQuote -> MarketPriceBlockState.Error(currencyName)
            is CryptoCurrencyStatus.Loaded,
            is CryptoCurrencyStatus.Custom,
            is CryptoCurrencyStatus.MissedDerivation,
            is CryptoCurrencyStatus.NoAccount,
            is CryptoCurrencyStatus.NoAmount,
            is CryptoCurrencyStatus.Unreachable,
            -> MarketPriceBlockState.Content(
                currencyName = currencyName,
                price = formatPrice(status, appCurrencyProvider()),
                priceChangeConfig = PriceChangeState.Content(
                    valueInPercent = formatPriceChange(status),
                    type = getPriceChangeType(status),
                ),
            )
        }
    }

    private fun getPriceChangeType(status: CryptoCurrencyStatus.Status): PriceChangeType {
        val priceChange = status.priceChange ?: return PriceChangeType.DOWN

        return if (priceChange > BigDecimal.ZERO) PriceChangeType.UP else PriceChangeType.DOWN
    }

    private fun formatPriceChange(status: CryptoCurrencyStatus.Status): String {
        val priceChange = status.priceChange ?: return BigDecimalFormatter.EMPTY_BALANCE_SIGN

        return BigDecimalFormatter.formatPercent(
            percent = priceChange,
            useAbsoluteValue = true,
        )
    }

    private fun formatPrice(status: CryptoCurrencyStatus.Status, appCurrency: AppCurrency): String {
        val fiatRate = status.fiatRate ?: return BigDecimalFormatter.EMPTY_BALANCE_SIGN

        return BigDecimalFormatter.formatFiatAmount(
            fiatAmount = fiatRate,
            fiatCurrencyCode = appCurrency.code,
            fiatCurrencySymbol = appCurrency.symbol,
        )
    }

    private fun formatFiatAmount(status: CryptoCurrencyStatus.Status, appCurrency: AppCurrency): String {
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
