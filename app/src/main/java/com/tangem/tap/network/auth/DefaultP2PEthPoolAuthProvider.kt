package com.tangem.tap.network.auth

import com.tangem.datasource.local.config.environment.EnvironmentConfigStorage
import com.tangem.lib.auth.P2PEthPoolAuthProvider

internal class DefaultP2PEthPoolAuthProvider(
    private val environmentConfigStorage: EnvironmentConfigStorage,
) : P2PEthPoolAuthProvider {

    override fun getApiKey(): String {
        // val keys = environmentConfigStorage.getConfigSync().p2pApiKey
        //     ?: error("No P2P api keys provided")
        //
        // return keys.mainnet

        environmentConfigStorage

        return "TODO restore p2p config call after release 5.30"
    }
}