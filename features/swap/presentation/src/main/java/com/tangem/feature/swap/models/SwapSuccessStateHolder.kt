package com.tangem.feature.swap.models

data class SwapSuccessStateHolder(
    val fromTokenAmount: String,
    val toTokenAmount: String,
    val onSecondaryButtonClick: () -> Unit,
)
