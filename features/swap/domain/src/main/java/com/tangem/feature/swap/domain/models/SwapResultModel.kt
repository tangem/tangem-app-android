package com.tangem.feature.swap.domain.models

sealed interface SwapResultModel {

    data class SwapSuccess(
        val fromTokenAmount: String,
        val toTokenAmount: String,
    ) : SwapResultModel

    data class SwapError(
        val errorCode: Int,
        val errorMessage: String,
    ) : SwapResultModel
}