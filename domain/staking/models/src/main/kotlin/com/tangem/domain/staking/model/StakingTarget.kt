package com.tangem.domain.staking.model

import com.tangem.domain.staking.model.common.RewardInfo
import com.tangem.domain.staking.model.common.RewardType
import com.tangem.domain.staking.model.ethpool.P2PEthPoolVault
import com.tangem.domain.staking.model.stakekit.Yield

/**
 * Represents either a StakeKit Validator or a P2P ETH Pool Vault.
 */
sealed interface StakingTarget {

    /** Unique identifier (validator address or vault address) */
    val address: String

    /** Display name */
    val name: String

    /** Reward info (rate and type) */
    val rewardInfo: RewardInfo?

    /** Whether this target is preferred/recommended */
    val isPreferred: Boolean

    /** Whether this target is active and available for staking */
    val isActive: Boolean

    /** Image URL for display (validator logo or vault icon) */
    val image: String?

    /** Whether this is a strategic partner (shows special badge in UI) */
    val isStrategicPartner: Boolean

    /**
     * StakeKit Validator wrapper
     */
    data class Validator(val delegate: Yield.Validator) : StakingTarget {
        override val address: String = delegate.address
        override val name: String = delegate.name
        override val rewardInfo: RewardInfo? = delegate.rewardInfo
        override val isPreferred: Boolean = delegate.preferred
        override val isActive: Boolean = delegate.status == Yield.Validator.ValidatorStatus.ACTIVE
        override val image: String? = delegate.image
        override val isStrategicPartner: Boolean = delegate.isStrategicPartner
    }

    /**
     * P2P ETH Pool Vault wrapper
     */
    data class Vault(val vault: P2PEthPoolVault) : StakingTarget {
        override val address: String = vault.vaultAddress
        override val name: String = vault.displayName
        override val rewardInfo = RewardInfo(
            rate = vault.apy,
            type = RewardType.APY,
        )
        override val isPreferred: Boolean = true
        override val isActive: Boolean = true
        override val image: String? = null
        override val isStrategicPartner: Boolean = true
    }
}

fun Yield.Validator.toStakingTarget(): StakingTarget = StakingTarget.Validator(this)

fun P2PEthPoolVault.toStakingTarget(): StakingTarget = StakingTarget.Vault(this)