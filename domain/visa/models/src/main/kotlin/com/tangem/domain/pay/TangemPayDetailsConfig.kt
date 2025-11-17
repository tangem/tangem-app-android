package com.tangem.domain.pay

import kotlinx.serialization.Serializable

@Serializable
data class TangemPayDetailsConfig(
    val cardId: String,
    val isCardFrozen: Boolean,
    val customerWalletAddress: String,
    val cardNumberEnd: String,
    val chainId: Int,
    val depositAddress: String?,
)