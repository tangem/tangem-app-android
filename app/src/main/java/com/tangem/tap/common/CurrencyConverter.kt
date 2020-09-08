package com.tangem.tap.common

import com.tangem.common.extensions.isZero
import java.math.BigDecimal
import java.math.RoundingMode

/**
[REDACTED_AUTHOR]
 */
class CurrencyConverter(var rateValue: BigDecimal) {

    fun toFiat(crypto: BigDecimal, decimals: Int = 2): BigDecimal {
        return rateValue.multiply(crypto).setScale(decimals, RoundingMode.DOWN)
    }

    fun toCrypto(fiat: BigDecimal, decimals: Int): BigDecimal {
        if (fiat.isZero() || rateValue.isZero()) return fiat

        val scaledRateValue = rateValue.setScale(decimals, RoundingMode.DOWN)
        val scaledFiat = fiat.setScale(decimals, RoundingMode.DOWN)
        return scaledFiat.divide(scaledRateValue, RoundingMode.UP)
    }
}