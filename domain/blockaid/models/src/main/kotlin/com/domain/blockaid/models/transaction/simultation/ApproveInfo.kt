package com.domain.blockaid.models.transaction.simultation

import java.math.BigDecimal

sealed class ApproveInfo {
    data class Amount(
        val approvedAmount: BigDecimal,
        val isUnlimited: Boolean,
        val tokenInfo: TokenInfo,
    ) : ApproveInfo()

    data class NonFungibleToken(val name: String, val logoUrl: String?) : ApproveInfo()
}