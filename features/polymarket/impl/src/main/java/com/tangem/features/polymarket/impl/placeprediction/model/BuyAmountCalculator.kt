package com.tangem.features.polymarket.impl.placeprediction.model

import com.tangem.domain.polymarket.model.PredictionOrderQuote
import com.tangem.features.polymarket.impl.common.PolymarketConst
import com.tangem.features.polymarket.impl.placeprediction.entity.BuyAmountFractionUM
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

internal object BuyAmountCalculator {

    private val HUNDRED = BigDecimal(100)

    fun fraction(balance: BigDecimal, fraction: BuyAmountFractionUM, lastQuote: PredictionOrderQuote?): BigDecimal {
        val share = balance.multiply(BigDecimal(fraction.percent)).divide(HUNDRED)
        val amount = if (fraction == BuyAmountFractionUM.Max) {
            share.min(balance.divide(BigDecimal.ONE + lastQuote.feeRate(), MathContext.DECIMAL64))
        } else {
            share
        }
        return amount.setScale(PolymarketConst.COLLATERAL_DISPLAY_DECIMALS, RoundingMode.DOWN)
    }

    private fun PredictionOrderQuote?.feeRate(): BigDecimal {
        val quote = this?.takeIf { it.notional.signum() > 0 } ?: return BigDecimal.ZERO
        return quote.fees.total.divide(quote.notional, MathContext.DECIMAL64)
    }
}