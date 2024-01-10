package com.tangem.feature.swap.domain.models.domain

data class PairsWithProviders(
    val pairs: List<SwapPairLeast>,
    val allProviders: List<SwapProvider>,
)