package com.tangem.domain.pay.model

import java.math.BigDecimal

data class TangemPayCardBalance(
    val fiatBalance: BigDecimal,
    val currencyCode: String,
    val cryptoBalance: BigDecimal,
    val chainId: Int,
    val depositAddress: String,
    val contractAddress: String,
)