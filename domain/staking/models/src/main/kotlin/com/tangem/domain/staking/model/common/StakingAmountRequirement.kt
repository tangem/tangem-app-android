package com.tangem.domain.staking.model.common

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable

/**
 * Provider-agnostic representation of staking amount requirements.
 * Contains validation constraints for stake/unstake amounts.
 *
 * Maps from:
 * - StakeKit: AddressArgument with ArgType.AMOUNT
 * - P2PEthPool: P2PEthPoolStaking.Metadata minimumStake/maximumStake
 */
@Serializable
data class StakingAmountRequirement(
    val isRequired: Boolean,
    val minimum: SerializedBigDecimal? = null,
    val maximum: SerializedBigDecimal? = null,
)