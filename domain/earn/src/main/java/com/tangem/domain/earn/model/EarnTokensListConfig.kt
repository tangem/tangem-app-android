package com.tangem.domain.earn.model

import com.tangem.domain.models.earn.EarnType
import kotlinx.serialization.Serializable

/**
 * Config of request earn tokens.
 *
 * @param type of Earn, null for every type.
 * @param networks list of networkId.
 * @param isForEarn flag to load mostly used tokens.
 * @param isVaultsIncluded vaults open only in the Earn flow, so they are left out while it is off.
 */
@Serializable
data class EarnTokensListConfig(
    val type: EarnType?,
    val networks: List<String>?,
    val isForEarn: Boolean,
    val isVaultsIncluded: Boolean,
)