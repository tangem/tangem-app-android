package com.tangem.domain.staking.model.common

import kotlinx.serialization.Serializable

/**
 * Provider-agnostic representation of staking action arguments.
 * Contains amount requirements and constraints for enter/exit operations.
 *
 * Maps from:
 * - StakeKit: Yield.Args.Enter
 * - P2PEthPool: P2PEthPoolStaking.Metadata
 */
@Serializable
data class StakingActionArgs(
    val amountRequirement: StakingAmountRequirement?,
    val isPartialAmountDisabled: Boolean,
)