package com.tangem.feature.swap.domain.models

data class QuoteModel(
    val fromTokenAmount: String,
    val toTokenAmount: String,
    val estimatedGas: Int
)
