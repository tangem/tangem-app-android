package com.tangem.features.swap.v2.impl.amount.model

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.swap.models.SwapDirection
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountFieldUM
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountType
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.utils.extensions.isZero
import java.math.BigDecimal
import java.math.RoundingMode

internal object SwapAmountQuoteUtils {

    fun calculatePriceImpact(
        fromTokenAmount: BigDecimal,
        toTokenAmount: BigDecimal,
        swapDirection: SwapDirection,
        primaryCryptoCurrencyStatus: CryptoCurrencyStatus,
        secondaryCryptoCurrencyStatus: CryptoCurrencyStatus,
    ): TextReference? {
        val (fromRate, toRate) = if (swapDirection == SwapDirection.Direct) {
            primaryCryptoCurrencyStatus.value.fiatRate to secondaryCryptoCurrencyStatus.value.fiatRate
        } else {
            secondaryCryptoCurrencyStatus.value.fiatRate to primaryCryptoCurrencyStatus.value.fiatRate
        }

        val fromTokenFiatValue = fromTokenAmount.multiply(fromRate)
        val toTokenFiatValue = toTokenAmount.multiply(toRate)

        // Check for zero division
        if (fromTokenFiatValue.isZero() || toTokenFiatValue.isZero()) return null

        val value = BigDecimal.ONE - toTokenFiatValue.divide(fromTokenFiatValue, 2, RoundingMode.HALF_UP)

        return stringReference("$(-${value.format { percent(withoutSign = false) }})").takeIf {
            value > 0.1.toBigDecimal()
        }
    }

    fun SwapAmountUM.updateAmount(
        onPrimaryAmount: SwapAmountFieldUM.Content.() -> SwapAmountFieldUM,
        onSecondaryAmount: SwapAmountFieldUM.Content.() -> SwapAmountFieldUM,
    ): SwapAmountUM {
        if (this !is SwapAmountUM.Content) return this

        return if (
            selectedAmountType == SwapAmountType.From && swapDirection == SwapDirection.Direct
        ) {
            val amountFieldUM = primaryAmount as? SwapAmountFieldUM.Content ?: return this
            copy(primaryAmount = amountFieldUM.onPrimaryAmount())
        } else {
            val amountFieldUM = secondaryAmount as? SwapAmountFieldUM.Content ?: return this
            copy(secondaryAmount = amountFieldUM.onSecondaryAmount())
        }
    }
}