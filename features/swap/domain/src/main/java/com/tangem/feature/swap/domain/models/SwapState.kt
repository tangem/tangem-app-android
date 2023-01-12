package com.tangem.feature.swap.domain.models

sealed interface SwapState {

    data class QuotesLoadedState(
        val fromTokenAmount: SwapAmount,
        val toTokenAmount: SwapAmount,
        val fromTokenAddress: String,
        val toTokenAddress: String,
        val fromTokenWalletBalance: String,
        val fromTokenFiatBalance: String,
        val toTokenWalletBalance: String,
        val toTokenFiatBalance: String,
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