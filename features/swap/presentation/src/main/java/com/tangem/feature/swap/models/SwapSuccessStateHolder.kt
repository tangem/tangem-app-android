package com.tangem.feature.swap.models

data class SwapSuccessStateHolder(
    val message: String,
    val onSecondaryButtonClick: () -> Unit,
)