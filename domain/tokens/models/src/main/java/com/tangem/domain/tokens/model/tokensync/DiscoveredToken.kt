package com.tangem.domain.tokens.model.tokensync

import java.math.BigDecimal

data class DiscoveredToken(
    val contractAddress: String?,
    val symbol: String,
    val name: String,
    val decimals: Int,
    val amount: BigDecimal,
    val isNativeToken: Boolean,
    val currencyId: String?,
    val networkId: String,
)