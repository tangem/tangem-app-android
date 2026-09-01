package com.tangem.domain.polymarket.model

import java.math.BigDecimal
import java.math.BigInteger

data class PredictionOrderDraft(
    val tokenId: String,
    val isNegRisk: Boolean,
    val side: PredictionOrderSide,
    val makerAmount: BigInteger,
    val takerAmount: BigInteger,
    val worstCasePrice: BigDecimal,
    val builderCode: String,
)

sealed interface PredictionOrderDraftError : PolymarketError {

    data class NotPlaceable(val status: PredictionQuoteStatus) : PredictionOrderDraftError

    data class SideMismatch(
        val quoted: PredictionOrderSide,
        val requested: PredictionOrderSide,
    ) : PredictionOrderDraftError

    data class AmountNotRepresentable(val value: BigDecimal) : PredictionOrderDraftError
}