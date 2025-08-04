package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.marketprice.PriceChangeState
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.components.marketprice.utils.PriceChangeConverter
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.utils.converter.Converter

internal class SingleWalletMarketPriceConverter(
    private val status: CryptoCurrencyStatus.Value,
    private val appCurrency: AppCurrency,
) : Converter<MarketPriceBlockState, MarketPriceBlockState> {

    override fun convert(value: MarketPriceBlockState): MarketPriceBlockState {
        return when (status) {
            CryptoCurrencyStatus.Loading -> MarketPriceBlockState.Loading(value.currencySymbol)
            is CryptoCurrencyStatus.NoAccount -> value.toNoAccountState()
            is CryptoCurrencyStatus.Loaded,
            is CryptoCurrencyStatus.NoAmount,
            -> value.toContentState()
            is CryptoCurrencyStatus.Custom,
            is CryptoCurrencyStatus.MissedDerivation,
            is CryptoCurrencyStatus.NoQuote,
            is CryptoCurrencyStatus.Unreachable,
            -> MarketPriceBlockState.Error(value.currencySymbol)
        }
    }

    private fun MarketPriceBlockState.toNoAccountState(): MarketPriceBlockState {
        return if (status.fiatRate == null) MarketPriceBlockState.Error(currencySymbol) else toContentState()
    }

    private fun MarketPriceBlockState.toContentState(): MarketPriceBlockState {
        return MarketPriceBlockState.Content(
            currencySymbol = currencySymbol,
            price = formatPrice(status = status, appCurrency = appCurrency),
            priceChangeConfig = PriceChangeState.Content(
                valueInPercent = formatPriceChange(status = status),
                type = getPriceChangeType(status = status),
            ),
        )
    }

    private fun formatPrice(status: CryptoCurrencyStatus.Value, appCurrency: AppCurrency): String {
        val fiatRate = status.fiatRate ?: return BigDecimalFormatter.EMPTY_BALANCE_SIGN

        return BigDecimalFormatter.formatFiatAmountUncapped(
            fiatAmount = fiatRate,
            fiatCurrencyCode = appCurrency.code,
            fiatCurrencySymbol = appCurrency.symbol,
        )
    }

    private fun formatPriceChange(status: CryptoCurrencyStatus.Value): String {
        val priceChange = status.priceChange ?: return BigDecimalFormatter.EMPTY_BALANCE_SIGN

        return priceChange.format { percent() }
    }

    private fun getPriceChangeType(status: CryptoCurrencyStatus.Value): PriceChangeType {
        return PriceChangeConverter.fromBigDecimal(status.priceChange)
    }
}