package com.tangem.feature.swap.domain.models.data

sealed interface SwapState {

    data class QuoteModel(
        val fromTokenAmount: SwapAmount,
        val toTokenAmount: SwapAmount,
        val fromTokenAddress: String,
        val toTokenAddress: String,
        val estimatedGas: Int,
        val isAllowedToSpend: Boolean = false,
    ) : SwapState

    data class SwapSuccess(
        val fromTokenAmount: SwapAmount,
        val toTokenAmount: SwapAmount,
    ) : SwapState

    data class SwapError(
        val errorType: DataError,
    ) : SwapState
}