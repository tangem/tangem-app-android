package com.tangem.common.ui.swap

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import com.tangem.core.ui.extensions.appendSpace
import com.tangem.core.ui.format.bigdecimal.anyDecimals
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.utils.StringsSigns
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.min

/**
 * Formats a swap exchange rate as `1 {base} ≈ {rate} {quote}`.
 *
 * The base/quote choice follows the rules in [SwapRateDirectionResolver] ([REDACTED_TASK_KEY]).
 */
object SwapRateFormatter {

    private const val MAX_DECIMALS_TO_SHOW = 8
    private const val IF_ZERO_DECIMALS_TO_SHOW = 2

    fun formatRate(from: CryptoCurrency, to: CryptoCurrency, fromAmount: BigDecimal, toAmount: BigDecimal): String {
        val (base, quote, rate) = computeRate(
            from = from,
            to = to,
            fromAmount = fromAmount,
            toAmount = toAmount,
        )
        return buildString {
            append(BigDecimal.ONE.format { crypto(symbol = base.symbol, decimals = 0).anyDecimals() })
            append(StringsSigns.WHITE_SPACE)
            append(StringsSigns.APPROXIMATE)
            append(StringsSigns.WHITE_SPACE)
            append(rate.format { crypto(quote) })
        }
    }

    fun formatRateAnnotated(
        from: CryptoCurrency,
        to: CryptoCurrency,
        fromAmount: BigDecimal,
        toAmount: BigDecimal,
    ): AnnotatedString {
        val (base, quote, rate) = computeRate(
            from = from,
            to = to,
            fromAmount = fromAmount,
            toAmount = toAmount,
        )
        return buildAnnotatedString {
            append(BigDecimal.ONE.format { crypto(symbol = base.symbol, decimals = 0).anyDecimals() })
            appendSpace()
            append(StringsSigns.APPROXIMATE)
            appendSpace()
            append(rate.format { crypto(quote) })
        }
    }

    private fun computeRate(
        from: CryptoCurrency,
        to: CryptoCurrency,
        fromAmount: BigDecimal,
        toAmount: BigDecimal,
    ): RateComputation {
        val direction = SwapRateDirectionResolver.resolve(from, to)
        val baseAmount: BigDecimal
        val quoteAmount: BigDecimal
        if (direction.base == from) {
            baseAmount = fromAmount
            quoteAmount = toAmount
        } else {
            baseAmount = toAmount
            quoteAmount = fromAmount
        }
        val rate = if (baseAmount.signum() == 0) {
            BigDecimal.ZERO
        } else {
            val rateDecimals = if (direction.quote.decimals == 0) IF_ZERO_DECIMALS_TO_SHOW else direction.quote.decimals
            quoteAmount.divide(baseAmount, min(rateDecimals, MAX_DECIMALS_TO_SHOW), RoundingMode.HALF_UP)
        }
        return RateComputation(direction.base, direction.quote, rate)
    }

    private data class RateComputation(val base: CryptoCurrency, val quote: CryptoCurrency, val rate: BigDecimal)
}