package com.tangem.feature.swap.domain.models.domain

data class SwapFeedbackParams(
    val userWalletIdHash: String,
    val providerName: String,
    val txUrl: String,
    val txExternalId: String,
    val rating: Int,
    val feedback: String,
)