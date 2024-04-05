package com.tangem.blockchainsdk.config

import com.tangem.blockchain.common.BlockchainSdkConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Runtime storage for [BlockchainSdkConfig]
 *
* [REDACTED_AUTHOR]
 */
internal class RuntimeConfigStorage : ConfigStorage {

    private val configFlow = MutableStateFlow(value = BlockchainSdkConfig())

    override fun get(): Flow<BlockchainSdkConfig> = configFlow

    override suspend fun store(config: BlockchainSdkConfig) {
        configFlow.value = config
    }
}
