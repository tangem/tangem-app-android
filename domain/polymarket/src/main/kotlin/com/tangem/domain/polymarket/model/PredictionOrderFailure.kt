package com.tangem.domain.polymarket.model

sealed interface PredictionOrderFailure : PolymarketError {

    data class Draft(val error: PredictionOrderDraftError) : PredictionOrderFailure

    data class Invalid(val error: PredictionOrderValidationError) : PredictionOrderFailure

    data class Signing(val error: PolymarketSigningError) : PredictionOrderFailure
}