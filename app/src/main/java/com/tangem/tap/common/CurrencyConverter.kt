package com.tangem.tap.common

import com.tangem.common.extensions.isZero
import com.tangem.tap.common.extensions.scaleToFiat
import java.math.BigDecimal
import java.math.RoundingMode

/**
[REDACTED_AUTHOR]
 */
class CurrencyConverter(
        private val rateValue: BigDecimal,
        private val decimals: Int
) {
    private val roundingMode = RoundingMode.DOWN

    fun toFiat(crypto: BigDecimal, fiatDecimals: Int = 2): BigDecimal {
        return toFiatUnscaled(crypto).setScale(fiatDecimals, roundingMode)
    }

    fun toFiatUnscaled(crypto: BigDecimal): BigDecimal {
        return rateValue.multiply(crypto).setScale(decimals, roundingMode)
    }

    fun toFiatWithPrecision(crypto: BigDecimal): BigDecimal {
        return toFiatUnscaled(crypto).scaleToFiat(true)
    }

    fun toCrypto(fiat: BigDecimal): BigDecimal {
        if (fiat.isZero()) return fiat

        val scaledRateValue = rateValue.setScale(decimals, roundingMode)
        val scaledFiat = fiat.setScale(decimals, roundingMode)
        return scaledFiat.divide(scaledRateValue, RoundingMode.UP)
    }
}