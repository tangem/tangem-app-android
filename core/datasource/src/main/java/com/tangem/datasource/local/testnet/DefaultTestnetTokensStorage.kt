package com.tangem.datasource.local.testnet

import com.squareup.moshi.JsonAdapter
import com.tangem.datasource.asset.reader.AssetReader
import com.tangem.datasource.local.testnet.models.TestnetTokensConfig

/**
 * Default implementation for storing testnet tokens data
 *
 * @property assetReader file reader from assets
 * @property adapter     adapter for parsing testnet tokens config
 *
* [REDACTED_AUTHOR]
 */
internal class DefaultTestnetTokensStorage(
    private val assetReader: AssetReader,
    private val adapter: JsonAdapter<TestnetTokensConfig>,
) : TestnetTokensStorage {

    @Deprecated(message = "Use AssetReader instead")
    override fun getConfig(): TestnetTokensConfig {
        return requireNotNull(
            value = adapter.fromJson(
                assetReader.readJson(fileName = LOCAL_CONFIG_PATH),
            ),
        )
    }

    private companion object {
        const val LOCAL_CONFIG_PATH: String = "testnet_tokens"
    }
}
