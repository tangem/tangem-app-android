package com.tangem.domain.dynamicaddresses.model

import java.math.BigDecimal

data class ConsolidationInfo(
    val fee: BigDecimal,
    val inputCount: Int,
    val canCoverFee: Boolean,
)