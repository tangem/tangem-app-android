package com.tangem.domain.yield.supply.models

import java.math.BigDecimal

data class YieldSupplyFee(
    val value: BigDecimal,
    val isHighFee: Boolean = false,
)