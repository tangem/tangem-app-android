package com.tangem.tap.network.auth

import com.tangem.datasource.local.config.environment.EnvironmentConfigStorage
import com.tangem.domain.staking.model.ethpool.P2PStakingConfig
import com.tangem.lib.auth.P2PEthPoolAuthProvider

internal class DefaultP2PEthPoolAuthProvider(
    private val environmentConfigStorage: EnvironmentConfigStorage,
) : P2PEthPoolAuthProvider {

    override fun getApiKey(): String {
        val keys = environmentConfigStorage.getConfigSync().p2pApiKey
            ?: error("No P2P api keys provided")

        return if (P2PStakingConfig.USE_TESTNET) keys.hoodi else keys.mainnet
    }
}