package com.tangem.features.send.mcp.model

import com.tangem.core.ui.format.bigdecimal.anyDecimals
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.utils.converter.Converter
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

/**
 * The amount a send intent asks for, as the sheets say it. It rounds up, as the amount is spent; sub-display-unit
 * values show a less-than bound. Returns `null` when the amount is not a positive number.
 */
internal class SendIntentAmountTextConverter @Inject constructor() :
    Converter<SendIntentAmountTextConverter.Input, String?> {

    data class Input(val amount: BigDecimal, val currency: CryptoCurrency)

    override fun convert(value: Input): String? {
        val amount = value.amount.takeIf { it.signum() > 0 } ?: return null
        val maxDecimals = value.currency.displayDecimals.coerceAtMost(MAX_DECIMALS)

        return amount.format {
            crypto(value.currency, ignoreSymbolPosition = true).anyDecimals(
                maxDecimals = maxDecimals,
                minDecimals = maxDecimals.coerceAtMost(MIN_DECIMALS),
                roundingMode = RoundingMode.UP,
                canBeLower = true,
            )
        }
    }

    private companion object {
        const val MAX_DECIMALS = 8
        const val MIN_DECIMALS = 2
    }
}