package com.tangem.tap.network.auth

import com.tangem.datasource.local.config.environment.EnvironmentConfig
import com.tangem.domain.staking.model.ethpool.P2PEthPoolStakingConfig
import com.tangem.lib.auth.P2PEthPoolAuthProvider

internal class DefaultP2PEthPoolAuthProvider(
    private val environmentConfig: EnvironmentConfig,
) : P2PEthPoolAuthProvider {

    override fun getApiKey(): String {
        val keys = environmentConfig.p2pApiKey
            ?: error("No P2P api keys provided")

        return if (P2PEthPoolStakingConfig.USE_TESTNET) keys.hoodi else keys.mainnet
    }
}