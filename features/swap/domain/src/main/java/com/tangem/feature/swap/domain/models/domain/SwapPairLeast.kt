package com.tangem.feature.swap.domain.models.domain

import com.tangem.domain.tokens.model.CryptoCurrencyStatus

/**
 * Domain layer representation of SwapPair data network model.
 *
 * @property from Short information about the token we want to change
 * @property to Short information about the token we want to exchange for
 * @property providers Exchange providers
 */
data class SwapPairLeast(
    val from: LeastTokenInfo,
    val to: LeastTokenInfo,
    val providers: List<SwapProvider>,
)

data class CryptoCurrencySwapInfo(
    val currencyStatus: CryptoCurrencyStatus,
    val providers: List<SwapProvider>,
)

/**
 * Provider that could swap given cryptocurrencies
 *
 * @property providerId provider id
 * @property rateTypes supported rate types
 */
data class SwapProvider(
    val providerId: Int,
    val rateTypes: List<RateType>,
)

/**
 * Rate type.
 *
 * Current implementation contains only float type, fixed will be supported later.
 */
enum class RateType {
    FLOAT,
    FIXED,
}
