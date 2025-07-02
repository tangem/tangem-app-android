package com.domain.blockaid.models.transaction.simultation

import java.math.BigDecimal

sealed class AmountInfo {

    data class FungibleTokens(
        val amount: BigDecimal,
        val token: TokenInfo,
    ) : AmountInfo()

    data class NonFungibleTokens(
        val name: String,
        val logoUrl: String?,
    ) : AmountInfo()
}