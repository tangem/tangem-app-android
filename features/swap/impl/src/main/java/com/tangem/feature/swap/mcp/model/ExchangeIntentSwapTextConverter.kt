package com.tangem.feature.swap.mcp.model

import com.tangem.core.ui.format.bigdecimal.anyDecimals
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.models.ExchangeIntent
import com.tangem.domain.swap.models.SwapAmountType
import com.tangem.feature.swap.mcp.ExchangeIntentAmountConverter
import com.tangem.utils.converter.Converter
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

/**
 * The two sides of the swap an intent asks for, as the sheets say them: the side with the amount shows it, the other
 * shows its symbol. The fixed side rounds spend up and receive down; sub-display-unit values show a less-than bound.
 * Returns `null` when the intent amount is not a positive number.
 */
internal class ExchangeIntentSwapTextConverter @Inject constructor(
    private val amountConverter: ExchangeIntentAmountConverter,
) : Converter<ExchangeIntentSwapTextConverter.Input, ExchangeIntentSwapTextConverter.SwapText?> {

    data class Input(val intent: ExchangeIntent, val from: CryptoCurrency, val to: CryptoCurrency)

    data class SwapText(val from: String, val to: String)

    override fun convert(value: Input): SwapText? {
        val intent = value.intent
        val amount = amountConverter.convert(intent)?.takeIf { it.signum() > 0 } ?: return null
        val fixedCurrency = if (intent.amountType == SwapAmountType.From) value.from else value.to
        val roundingMode = when (intent.amountType) {
            SwapAmountType.From -> RoundingMode.UP
            SwapAmountType.To -> RoundingMode.DOWN
        }
        val fixedAmount = amount.toOfferAmount(currency = fixedCurrency, roundingMode = roundingMode)

        return when (intent.amountType) {
            SwapAmountType.From -> SwapText(from = fixedAmount, to = value.to.symbol)
            SwapAmountType.To -> SwapText(from = value.from.symbol, to = fixedAmount)
        }
    }

    private fun BigDecimal.toOfferAmount(currency: CryptoCurrency, roundingMode: RoundingMode): String {
        val maxDecimals = currency.displayDecimals.coerceAtMost(MAX_DECIMALS)

        return format {
            crypto(currency, ignoreSymbolPosition = true).anyDecimals(
                maxDecimals = maxDecimals,
                minDecimals = maxDecimals.coerceAtMost(MIN_DECIMALS),
                roundingMode = roundingMode,
                canBeLower = true,
            )
        }
    }

    private companion object {
        const val MAX_DECIMALS = 8
        const val MIN_DECIMALS = 2
    }
}