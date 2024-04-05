package com.tangem.blockchainsdk.config

import com.tangem.blockchain.common.BlockchainSdkConfig
import kotlinx.coroutines.flow.Flow

/**
 * Storage for [BlockchainSdkConfig]
 *
[REDACTED_AUTHOR]
 */
internal interface ConfigStorage {

    /** Get flow of [BlockchainSdkConfig] */
    fun get(): Flow<BlockchainSdkConfig>

    /** Store [BlockchainSdkConfig] */
    suspend fun store(config: BlockchainSdkConfig)
}