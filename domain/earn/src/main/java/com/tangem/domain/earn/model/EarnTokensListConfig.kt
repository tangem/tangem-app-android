package com.tangem.domain.earn.model

import kotlinx.serialization.Serializable

/**
 * Config of request earn tokens.
 *
 * @param type of Earn (ex: yield, staking).
 * @param networks list of networkId.
 * @param isForEarn flag to load mostly used tokens.
 */
@Serializable
data class EarnTokensListConfig(
    val type: String?,
    val networks: List<String>?,
    val isForEarn: Boolean,
)