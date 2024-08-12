package com.tangem.features.markets.details.impl.model.converters

import androidx.compose.runtime.Stable
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.features.markets.details.impl.ui.state.PricePerformanceUM
import com.tangem.utils.Provider
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import java.math.BigDecimal
import java.math.RoundingMode

@Stable
internal class PricePerformanceConverter(
    private val appCurrency: Provider<AppCurrency>,
) : Converter<TokenMarketInfo.PricePerformance, PricePerformanceUM> {

    override fun convert(value: TokenMarketInfo.PricePerformance): PricePerformanceUM {
        return PricePerformanceUM(
            h24 = value.day.convert(),
            month = value.month.convert(),
            all = value.allTime.convert(),
        )
    }

    private fun TokenMarketInfo.Range?.convert(): PricePerformanceUM.Value {
        if (this == null) {
            return PricePerformanceUM.Value(
                low = StringsSigns.DASH_SIGN,
                high = StringsSigns.DASH_SIGN,
                indicatorFraction = 0f,
            )
        }

        return PricePerformanceUM.Value(
            low = low.convert(),
            high = high.convert(),
            indicatorFraction = calculateFraction(),
        )
    }

    private fun BigDecimal?.convert(): String {
        val currency = appCurrency()

        return BigDecimalFormatter.formatFiatPriceUncapped(
            fiatAmount = this,
            fiatCurrencyCode = currency.code,
            fiatCurrencySymbol = currency.symbol,
        )
    }

    private fun TokenMarketInfo.Range.calculateFraction(): Float {
        if (low == null || high == null || low == BigDecimal.ZERO) return 0f
        return (high!! - low!!).divide(low!!, RoundingMode.HALF_UP)
            .setScale(2, RoundingMode.HALF_UP)
            .toFloat().coerceAtMost(1f)
    }
}
