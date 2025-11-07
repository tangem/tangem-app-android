package com.tangem.domain.yield.supply.models

import java.math.BigDecimal

data class YieldSupplyMaxFee(
    val nativeMaxFee: BigDecimal,
    val tokenMaxFee: BigDecimal,
    val fiatMaxFee: BigDecimal,
)