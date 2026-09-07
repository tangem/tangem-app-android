package com.tangem.data.managetokens.testnet

import com.tangem.core.local.assets.AssetLoader
import com.tangem.core.local.assets.load
import com.tangem.data.managetokens.testnet.models.TestnetTokensConfig

internal class DefaultTestnetTokensStorage(
    private val assetLoader: AssetLoader,
) : TestnetTokensStorage {

    override suspend fun getConfig(): TestnetTokensConfig {
        return requireNotNull(assetLoader.load<TestnetTokensConfig>(fileName = LOCAL_CONFIG_PATH))
    }

    private companion object {
        const val LOCAL_CONFIG_PATH: String = "testnet_tokens"
    }
}