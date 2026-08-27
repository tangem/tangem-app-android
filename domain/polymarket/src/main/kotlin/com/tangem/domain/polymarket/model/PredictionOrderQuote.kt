package com.tangem.domain.polymarket.model

import java.math.BigDecimal

data class PredictionOrderQuoteRequest(
    val marketId: String,
    val assetId: String,
    val side: PredictionOrderSide,
    val amount: BigDecimal,
    val slippagePercent: BigDecimal?,
)

data class PredictionOrderQuote(
    val status: PredictionQuoteStatus,
    val side: PredictionOrderSide,
    val shares: BigDecimal,
    val notional: BigDecimal,
    val expectedExecutionAmount: BigDecimal,
    val averagePrice: BigDecimal,
    val worstCasePrice: BigDecimal,
    val fees: PredictionOrderFees,
    val total: BigDecimal,
    val builderCode: String,
    val minOrderSize: BigDecimal,
    val tickSize: BigDecimal,
    val isLive: Boolean,
)

data class PredictionOrderFees(
    val market: BigDecimal,
    val builder: BigDecimal,
    val total: BigDecimal,
)