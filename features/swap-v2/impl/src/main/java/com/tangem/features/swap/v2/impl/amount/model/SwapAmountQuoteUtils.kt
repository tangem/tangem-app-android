package com.tangem.features.swap.v2.impl.amount.model

import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.swap.models.SwapAmountType
import com.tangem.domain.swap.models.SwapDirection
import com.tangem.features.swap.v2.impl.amount.entity.PriceImpact
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountFieldUM
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.utils.StringsSigns
import com.tangem.utils.extensions.orZero
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.min

internal object SwapAmountQuoteUtils {

    private const val MAX_DECIMALS = 8
    private const val MIN_DECIMALS = 2

    private val PRICE_IMPACT_AMOUNT_MIN_THRESHOLD = 25.toBigDecimal() // in USD
    private val PRICE_IMPACT_AMOUNT_MAX_THRESHOLD = 5000.toBigDecimal() // in USD
    private val PRICE_IMPACT_AMOUNT_LOW_THRESHOLD = 100_000.toBigDecimal() // in USD
    private val PRICE_IMPACT_LOW_THRESHOLD = 0.1.toBigDecimal() // 10%
    private val PRICE_IMPACT_HIGH_THRESHOLD = 0.5.toBigDecimal() // 50%

    @Suppress("ComplexMethod", "LongParameterList")
    internal fun calculatePriceImpact(
        quoteContent: SwapQuoteUM.Content?,
        swapDirection: SwapDirection,
        primaryFiatRateUSD: BigDecimal?,
        secondaryFiatRateUSD: BigDecimal?,
        primaryCryptoCurrencyStatus: CryptoCurrencyStatus,
        secondaryCryptoCurrencyStatus: CryptoCurrencyStatus?,
    ): PriceImpact? {
        if (quoteContent == null) return null

        val fromAmount = quoteContent.fromAmount ?: return null
        val toAmount = quoteContent.toAmount

        val (fromRate, toRate) = if (swapDirection == SwapDirection.Direct) {
            primaryCryptoCurrencyStatus.value.fiatRate to secondaryCryptoCurrencyStatus?.value?.fiatRate
        } else {
            secondaryCryptoCurrencyStatus?.value?.fiatRate to primaryCryptoCurrencyStatus.value.fiatRate
        }

        val (fromRateUsd, toRateUsd) = if (swapDirection == SwapDirection.Direct) {
            primaryFiatRateUSD to secondaryFiatRateUSD
        } else {
            secondaryFiatRateUSD to primaryFiatRateUSD
        }

        val fromTokenFiatValue = fromRate?.let { fromAmount.multiply(fromRate).orZero() }
        val toTokenFiatValue = toRate?.let { toAmount.multiply(toRate) }

        val isFromNotZero = fromTokenFiatValue != null && fromTokenFiatValue != BigDecimal.ZERO
        val isToNotZero = toTokenFiatValue != null && toTokenFiatValue != BigDecimal.ZERO

        val value = if (isFromNotZero && isToNotZero) {
            BigDecimal.ONE - toTokenFiatValue.divide(fromTokenFiatValue, 2, RoundingMode.HALF_UP)
        } else {
            null
        }

        val fromAmountUSD = fromAmount.multiply(fromRateUsd.orZero())
        val toAmountUSD = toAmount.multiply(toRateUsd.orZero())
        val amountDiff = fromAmountUSD - toAmountUSD

        val type = when {
            value == null -> PriceImpact.Type.NONE
            value < PRICE_IMPACT_LOW_THRESHOLD &&
                amountDiff <= PRICE_IMPACT_AMOUNT_LOW_THRESHOLD -> PriceImpact.Type.LOW
            value > PRICE_IMPACT_HIGH_THRESHOLD -> PriceImpact.Type.HIGH
            else -> PriceImpact.Type.MEDIUM
        }

        val amountSignificance = when {
            fromAmountUSD <= PRICE_IMPACT_AMOUNT_MIN_THRESHOLD -> PriceImpact.AmountSignificance.LOW
            fromAmountUSD > PRICE_IMPACT_AMOUNT_MAX_THRESHOLD -> PriceImpact.AmountSignificance.HIGH
            else -> PriceImpact.AmountSignificance.MEDIUM
        }

        return PriceImpact(
            value = stringReference("(${StringsSigns.MINUS}${value.format { percent() }})"),
            amountSignificance = amountSignificance,
            type = type,
        )
    }

    fun isHighPriceImpact(amountUM: SwapAmountUM): Boolean {
        val priceImpact = (amountUM as? SwapAmountUM.Content)?.priceImpact ?: return false
        return priceImpact.shouldDisableButton()
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

        val isPrimaryFieldEdited = selectedAmountType == SwapAmountType.From && swapDirection == SwapDirection.Direct

        val updatedAmountField = if (isPrimaryFieldEdited) {
            val amountFieldUM = primaryAmount as? SwapAmountFieldUM.Content ?: return this
            amountFieldUM.onPrimaryAmount(primaryCryptoCurrencyStatus)
        } else {
            if (secondaryCryptoCurrencyStatus == null) return this
            val amountFieldUM = secondaryAmount as? SwapAmountFieldUM.Content ?: return this
            amountFieldUM.onSecondaryAmount(secondaryCryptoCurrencyStatus)
        }
        return copy(
            primaryAmount = if (isPrimaryFieldEdited) updatedAmountField else primaryAmount,
            secondaryAmount = if (isPrimaryFieldEdited) secondaryAmount else updatedAmountField,
        )
    }
}