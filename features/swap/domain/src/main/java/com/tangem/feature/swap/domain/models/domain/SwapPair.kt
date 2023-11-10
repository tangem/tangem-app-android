package com.tangem.feature.swap.domain.models.domain

data class SwapPair(
    val from: LeastTokenInfo,
    val to: LeastTokenInfo,
    val providers: List<SwapPairProvider>,
)

data class SwapPairProvider(
    val providerId: Int,
    val rateTypes: List<RateType>,
)

enum class RateType {
    FLOAT,
    FIXED,
}
