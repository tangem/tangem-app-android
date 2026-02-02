package com.tangem.domain.staking.model

import com.tangem.domain.models.serialization.SerializedBigDecimal
import com.tangem.domain.models.staking.YieldToken
import com.tangem.domain.staking.model.ethpool.P2PEthPoolVault
import com.tangem.domain.staking.model.stakekit.Yield

/**
 * Represents a staking option from any provider
 * Unified abstraction over StakeKit and P2PEthPool staking integrations
 */
sealed interface StakingOption {

    /** Unique identifier for the staking option */
    val integrationId: StakingIntegrationID

    /** Annual Percentage Yield */
    val apy: SerializedBigDecimal

    /** Token being staked */
    val token: YieldToken // TODO p2p

    /** Whether this staking option is available */
    val isAvailable: Boolean

    /**
     * StakeKit staking option
     * Wraps StakeKit Yield with all validator and metadata information
     */
    data class StakeKit(
        override val integrationId: StakingIntegrationID.StakeKit,
        val yield: Yield,
    ) : StakingOption {
        override val apy: SerializedBigDecimal = yield.apy
        override val token: YieldToken = yield.token
        override val isAvailable: Boolean = yield.isAvailable
    }

    /**
     * P2PEthPool staking option
     * Wraps P2PEthPool vault information
     */
    data class P2PEthPool(val vaults: List<P2PEthPoolVault>) : StakingOption {
        override val integrationId: StakingIntegrationID = StakingIntegrationID.P2PEthPool
        override val apy: SerializedBigDecimal = vaults.maxOf { it.apy }
        override val token: YieldToken = YieldToken.ETH
        override val isAvailable: Boolean = vaults.isNotEmpty()
    }
}