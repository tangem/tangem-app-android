package com.domain.blockaid.models.transaction.simultation

import java.math.BigDecimal

data class ApprovedAmount(
    val approvedAmount: BigDecimal,
    val isUnlimited: Boolean,
    val tokenInfo: TokenInfo,
)