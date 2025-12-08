package com.tangem.domain.staking.model

import com.tangem.domain.models.serialization.SerializedBigDecimal
import com.tangem.domain.models.staking.NetworkType
import com.tangem.domain.models.staking.YieldToken
import com.tangem.domain.staking.model.ethpool.P2PEthPoolVault
import com.tangem.domain.staking.model.stakekit.Yield

/**
 * Represents a staking option from any provider
 * Unified abstraction over StakeKit and P2P staking integrations
 */
sealed interface StakingOption {

    /** Unique identifier for the staking option */
    val integrationId: String

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
    data class StakeKit(val yield: Yield) : StakingOption {
        override val integrationId: String = yield.id
        override val apy: SerializedBigDecimal = yield.apy
        override val token: YieldToken = yield.token
        override val isAvailable: Boolean = yield.isAvailable
    }

    /**
     * P2P pooled staking option
     * Wraps P2P ETH Pool vault information
     */
    data class P2P(val vaults: List<P2PEthPoolVault>) : StakingOption {
        override val integrationId: String = "p2p-ethereum-pooled"
        override val apy: SerializedBigDecimal = vaults.maxOf { it.apy }
        override val token: YieldToken = createEthToken()
        override val isAvailable: Boolean = vaults.isNotEmpty()

        private fun createEthToken(): YieldToken { // TODO
            return YieldToken(
                name = "Ethereum",
                network = NetworkType.ETHEREUM,
                symbol = "ETH",
                decimals = 18,
                address = null, // Native token
                coinGeckoId = "ethereum",
                logoURI = null,
                isPoints = false,
            )
        }
    }
}