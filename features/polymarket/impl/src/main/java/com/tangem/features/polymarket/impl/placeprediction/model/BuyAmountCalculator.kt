package com.tangem.features.polymarket.impl.placeprediction.model

import com.tangem.domain.polymarket.model.PredictionOrderQuote
import com.tangem.features.polymarket.impl.common.PolymarketConst
import com.tangem.features.polymarket.impl.placeprediction.entity.AmountFractionUM
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

internal object BuyAmountCalculator {

    private val HUNDRED = BigDecimal(100)
    private val MAX_SHARE_OF_BALANCE = BigDecimal("0.9")

    fun fraction(balance: BigDecimal, fraction: AmountFractionUM, feeRate: BigDecimal): BigDecimal {
        val amount = if (fraction == AmountFractionUM.Max) {
            balance.multiply(MAX_SHARE_OF_BALANCE).min(balance.divide(BigDecimal.ONE + feeRate, MathContext.DECIMAL64))
        } else {
            balance.multiply(BigDecimal(fraction.percent)).divide(HUNDRED)
        }
        return amount.setScale(PolymarketConst.COLLATERAL_DISPLAY_DECIMALS, RoundingMode.DOWN)
    }

    fun feeRate(quote: PredictionOrderQuote): BigDecimal? = quote.notional.takeIf { it.signum() > 0 }
        ?.let { notional -> quote.fees.total.divide(notional, MathContext.DECIMAL64) }
}