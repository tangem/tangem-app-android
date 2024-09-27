package com.tangem.datasource.local.config.testnet

import com.tangem.datasource.asset.loader.AssetLoader
import com.tangem.datasource.local.config.testnet.models.TestnetTokensConfig

/**
 * Default implementation for storing testnet tokens data
 *
 * @property assetLoader asset loader
 *
 * @author Andrew Khokhlov on 07/04/2023
 */
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
