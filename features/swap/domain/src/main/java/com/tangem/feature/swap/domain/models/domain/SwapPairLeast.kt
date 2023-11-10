package com.tangem.feature.swap.domain.models.domain

import com.tangem.domain.tokens.model.CryptoCurrency

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
    val providers: List<SwapPairProvider>,
)

/**
 * Enriched model of swap pair data. Contains full CryptoCurrency models instead of least info.
 *
 * @property from CryptoCurrency we want to change
 * @property to CryptoCurrency we want to exchange for
 * @property providers Exchange providers
 */
data class SwapPair(
    val from: CryptoCurrency,
    val to: CryptoCurrency,
    val providers: List<SwapPairProvider>,
)

/**
 *
 *
 * @property providerId
 * @property rateTypes
 */
data class SwapPairProvider(
    val providerId: Int,
    val rateTypes: List<RateType>,
)

/**
 * 
 *
 */
enum class RateType {
    FLOAT,
    FIXED,
}
