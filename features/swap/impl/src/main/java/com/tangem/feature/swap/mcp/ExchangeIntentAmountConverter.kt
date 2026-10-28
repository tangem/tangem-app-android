package com.tangem.feature.swap.mcp

import com.tangem.domain.swap.models.ExchangeIntent
import com.tangem.domain.swap.models.SwapAmountType
import com.tangem.utils.converter.Converter
import java.math.BigDecimal
import javax.inject.Inject

/**
 * The amount an [ExchangeIntent] fixes, as a decimal number: Express sends it in minor units of the intent's own asset
 * on one side only, so it is shifted by the decimals of that asset. `null` when the amount is not a number.
 */
internal class ExchangeIntentAmountConverter @Inject constructor() : Converter<ExchangeIntent, BigDecimal?> {

    override fun convert(value: ExchangeIntent): BigDecimal? {
        val asset = when (value.amountType) {
            SwapAmountType.From -> value.from
            SwapAmountType.To -> value.to
        }

        return value.amount.toBigDecimalOrNull()?.movePointLeft(asset.decimals)
    }
}