package com.tangem.tap.network.auth

import com.tangem.datasource.local.config.environment.EnvironmentConfigStorage
import com.tangem.lib.auth.P2PAuthProvider

internal class DefaultP2PAuthProvider(
    private val environmentConfigStorage: EnvironmentConfigStorage,
) : P2PAuthProvider {

    override fun getApiKey(): String {
        // val keys = environmentConfigStorage.getConfigSync().p2pApiKey
        //     ?: error("No P2P api keys provided")
        //
        // return keys.mainnet

        environmentConfigStorage

        return "TODO restore p2p config call after release 5.30"
    }
}