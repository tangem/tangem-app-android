package com.tangem.features.swap.v2.impl.amount.model

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.swap.models.SwapDirection
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountFieldUM
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountType
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.utils.isNullOrZero
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.min

internal object SwapAmountQuoteUtils {

    private const val MAX_DECIMALS = 8
    private const val MIN_DECIMALS = 2

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

        if (fromRate.isNullOrZero() || toRate.isNullOrZero()) return null

        val fromTokenFiatValue = fromTokenAmount.multiply(fromRate)
        val toTokenFiatValue = toTokenAmount.multiply(toRate)

        val value = BigDecimal.ONE - toTokenFiatValue.divide(fromTokenFiatValue, 2, RoundingMode.HALF_UP)

        return stringReference("$(-${value.format { percent(withoutSign = false) }})").takeIf {
            value > 0.1.toBigDecimal()
        }
    }

    fun calculateRate(fromAmount: BigDecimal, toAmount: BigDecimal, toAmountDecimals: Int): BigDecimal {
        val rateDecimals = if (toAmountDecimals == 0) MIN_DECIMALS else toAmountDecimals
        return toAmount.divide(fromAmount, min(rateDecimals, MAX_DECIMALS), RoundingMode.HALF_UP)
    }

    fun SwapAmountUM.updateAmount(
        onPrimaryAmount: SwapAmountFieldUM.Content.(CryptoCurrencyStatus) -> SwapAmountFieldUM,
        onSecondaryAmount: SwapAmountFieldUM.Content.(CryptoCurrencyStatus) -> SwapAmountFieldUM,
    ): SwapAmountUM {
        if (this !is SwapAmountUM.Content) return this

        return if (
            selectedAmountType == SwapAmountType.From && swapDirection == SwapDirection.Direct
        ) {
            val amountFieldUM = primaryAmount as? SwapAmountFieldUM.Content ?: return this
            copy(primaryAmount = amountFieldUM.onPrimaryAmount(primaryCryptoCurrencyStatus))
        } else {
            if (secondaryCryptoCurrencyStatus == null) return this
            val amountFieldUM = secondaryAmount as? SwapAmountFieldUM.Content ?: return this
            copy(secondaryAmount = amountFieldUM.onSecondaryAmount(secondaryCryptoCurrencyStatus))
        }
    }
}