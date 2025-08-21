package com.tangem.features.markets.details.impl.model.converters

import androidx.compose.runtime.Stable
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.price
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.PriceChangeInterval
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.features.markets.details.impl.ui.state.PricePerformanceUM
import com.tangem.utils.Provider
import com.tangem.utils.StringsSigns
import java.math.BigDecimal
import java.math.RoundingMode

@Stable
internal class PricePerformanceConverter(
    private val appCurrency: Provider<AppCurrency>,
    private val onIntervalChanged: (PriceChangeInterval) -> Unit,
) {

    fun convert(value: TokenMarketInfo.PricePerformance, currentPrice: BigDecimal): PricePerformanceUM {
        return PricePerformanceUM(
            h24 = value.day.convert(currentPrice),
            month = value.month.convert(currentPrice),
            all = value.allTime.convert(currentPrice),
            onIntervalChanged = onIntervalChanged,
        )
    }

    private fun TokenMarketInfo.Range?.convert(currentPrice: BigDecimal): PricePerformanceUM.Value {
        if (this == null || this.low == null || this.high == null) {
            return PricePerformanceUM.Value(
                low = StringsSigns.DASH_SIGN,
                high = StringsSigns.DASH_SIGN,
                indicatorFraction = 0f,
            )
        }

        return PricePerformanceUM.Value(
            low = low.convert(),
            high = high.convert(),
            indicatorFraction = calculateFraction(currentPrice),
        )
    }

    private fun BigDecimal?.convert(): String {
        val currency = appCurrency()

        return format {
            fiat(
                fiatCurrencyCode = currency.code,
                fiatCurrencySymbol = currency.symbol,
            ).price()
        }
    }

    private fun TokenMarketInfo.Range.calculateFraction(currentPrice: BigDecimal): Float {
        return when {
            low == null || high == null || high == BigDecimal.ZERO || currentPrice < low -> 0f
            currentPrice > high || low == high -> 1f
            else -> {
                (currentPrice - low!!).divide(high!! - low!!, RoundingMode.HALF_UP)
                    .setScale(2, RoundingMode.HALF_UP)
                    .toFloat().coerceAtMost(1f)
            }
        }
    }
}