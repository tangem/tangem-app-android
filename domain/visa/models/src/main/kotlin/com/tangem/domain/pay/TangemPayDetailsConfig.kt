package com.tangem.domain.pay

import com.tangem.domain.visa.model.TangemPayCardFrozenState
import kotlinx.serialization.Serializable

@Serializable
data class TangemPayDetailsConfig(
    val cardId: String,
    val cardFrozenState: TangemPayCardFrozenState,
    val customerWalletAddress: String,
    val cardNumberEnd: String,
    val chainId: Int,
)