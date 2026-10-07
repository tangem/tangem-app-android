package com.tangem.feature.swap.converters

import java.math.BigDecimal

/**
 * Express carries slippage with two decimals included: 0.5 % travels as `50`, in responses and in the
 * `customSlippage` request param alike. The domain works in plain percent.
 */
internal object ExpressSlippageConverter {

    private const val INCLUDED_DECIMALS = 2

    fun fromApi(value: BigDecimal?): BigDecimal? = value?.movePointLeft(INCLUDED_DECIMALS)

    fun toApi(percent: BigDecimal?): String? =
        percent?.movePointRight(INCLUDED_DECIMALS)?.stripTrailingZeros()?.toPlainString()
}