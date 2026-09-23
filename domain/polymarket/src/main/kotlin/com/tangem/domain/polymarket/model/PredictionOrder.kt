package com.tangem.domain.polymarket.model

import java.math.BigInteger

data class PredictionOrder(
    val salt: Long,
    val maker: String,
    val signer: String,
    val tokenId: BigInteger,
    val makerAmount: BigInteger,
    val takerAmount: BigInteger,
    val side: PredictionOrderSide,
    val signatureType: Int,
    val timestampMillis: Long,
    val metadata: String,
    val builder: String,
)