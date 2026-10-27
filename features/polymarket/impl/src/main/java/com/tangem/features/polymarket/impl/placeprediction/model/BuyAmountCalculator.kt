package com.tangem.features.polymarket.impl.placeprediction.model

import com.tangem.domain.polymarket.model.PredictionOrderQuote
import com.tangem.features.polymarket.impl.common.PolymarketConst
import com.tangem.features.polymarket.impl.placeprediction.entity.BuyAmountFractionUM
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

internal object BuyAmountCalculator {

    private val HUNDRED = BigDecimal(100)

    fun fraction(balance: BigDecimal, fraction: BuyAmountFractionUM, feeRate: BigDecimal): BigDecimal {
        val share = balance.multiply(BigDecimal(fraction.percent)).divide(HUNDRED)
        val amount = if (fraction == BuyAmountFractionUM.Max) {
            share.min(balance.divide(BigDecimal.ONE + feeRate, MathContext.DECIMAL64))
        } else {
            share
        }
        return amount.setScale(PolymarketConst.COLLATERAL_DISPLAY_DECIMALS, RoundingMode.DOWN)
    }

    fun feeRate(quote: PredictionOrderQuote): BigDecimal? = quote.notional.takeIf { it.signum() > 0 }
        ?.let { notional -> quote.fees.total.divide(notional, MathContext.DECIMAL64) }
}