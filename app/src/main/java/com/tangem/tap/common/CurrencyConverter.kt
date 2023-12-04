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
    private val decimals: Int,
) {
    private val roundingMode = RoundingMode.HALF_UP

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

        val fiatValue = fiat.setScale(rateValue.scale(), RoundingMode.UP)
        val cryptoValue = fiatValue.divide(rateValue, RoundingMode.UP)
        return cryptoValue.setScale(decimals, roundingMode)
    }
}