package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import arrow.core.Either
import com.tangem.common.Provider
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.marketprice.PriceChangeConfig
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.error.CurrencyError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.feature.tokendetails.presentation.tokendetails.TokenDetailsPreviewData
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsBalanceBlockState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

class TokenDetailsLoadedBalanceConverter(
    private val currentStateProvider: Provider<TokenDetailsState>,
    private val appCurrencyProvider: Provider<AppCurrency>,
) : Converter<Either<CurrencyError, CryptoCurrencyStatus>, TokenDetailsState> {

    override fun convert(value: Either<CurrencyError, CryptoCurrencyStatus>): TokenDetailsState {
        return value.fold(ifLeft = { convertError() }, ifRight = ::convert)
    }

    private fun convertError(): TokenDetailsState {
        // TODO:  [REDACTED_JIRA]
        return currentStateProvider()
    }

    private fun convert(status: CryptoCurrencyStatus): TokenDetailsState {
        val state = currentStateProvider()
        val currencyName = state.marketPriceBlockState.currencyName
        return state.copy(
            tokenBalanceBlockState = getBalanceState(status),
            marketPriceBlockState = getMarketPriceState(status = status.value, currencyName = currencyName),
        )
    }

    private fun getBalanceState(status: CryptoCurrencyStatus): TokenDetailsBalanceBlockState {
        return when (status.value) {
            is CryptoCurrencyStatus.NoQuote,
            is CryptoCurrencyStatus.Loaded,
            -> {
                TokenDetailsBalanceBlockState.Content(
                    actionButtons = TokenDetailsPreviewData.actionButtons,
                    fiatBalance = formatFiatAmount(status.value, appCurrencyProvider()),
                    cryptoBalance = formatCryptoAmount(status),
                )
            }
            is CryptoCurrencyStatus.Loading -> {
                TokenDetailsBalanceBlockState.Loading(TokenDetailsPreviewData.disabledActionButtons)
            }
            is CryptoCurrencyStatus.MissedDerivation,
            is CryptoCurrencyStatus.NoAccount,
            is CryptoCurrencyStatus.Custom,
            // TODO:  [REDACTED_JIRA]
            is CryptoCurrencyStatus.Unreachable,
            -> {
                TokenDetailsBalanceBlockState.Error(TokenDetailsPreviewData.actionButtons)
            }
        }
    }

    private fun getMarketPriceState(status: CryptoCurrencyStatus.Status, currencyName: String): MarketPriceBlockState {
        return when (status) {
            is CryptoCurrencyStatus.NoQuote,
            is CryptoCurrencyStatus.Loaded,
            -> MarketPriceBlockState.Content(
                currencyName = currencyName,
                price = formatPrice(status, appCurrencyProvider()),
                priceChangeConfig = PriceChangeConfig(
                    valueInPercent = formatPriceChange(status),
                    type = getPriceChangeType(status),
                ),
            )
            is CryptoCurrencyStatus.Loading -> MarketPriceBlockState.Loading(currencyName)
            is CryptoCurrencyStatus.Custom,
            is CryptoCurrencyStatus.MissedDerivation,
            is CryptoCurrencyStatus.NoAccount,
            is CryptoCurrencyStatus.Unreachable,
            -> MarketPriceBlockState.Error(currencyName)
        }
    }

    private fun getPriceChangeType(status: CryptoCurrencyStatus.Status): PriceChangeConfig.Type {
        val priceChange = status.priceChange ?: return PriceChangeConfig.Type.DOWN

        return if (priceChange > BigDecimal.ZERO) {
            PriceChangeConfig.Type.UP
        } else {
            PriceChangeConfig.Type.DOWN
        }
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