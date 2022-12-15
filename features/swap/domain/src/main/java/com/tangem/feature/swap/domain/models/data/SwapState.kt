package com.tangem.feature.swap.domain.models.data

sealed interface SwapState {

    data class QuoteModel(
        val fromTokenAmount: String,
        val toTokenAmount: String,
        val estimatedGas: Int,
        val isAllowedToSpend: Boolean = false,
    ) : SwapState

    data class SwapSuccess(
        val fromTokenAmount: String,
        val toTokenAmount: String,
    ) : SwapState

    data class SwapError(
        val errorType: DataError,
    ) : SwapState
}
