package com.tangem.blockchainsdk.config

import com.tangem.blockchain.common.BlockchainSdkConfig
import kotlinx.coroutines.flow.Flow

/**
 * Storage for [BlockchainSdkConfig]
 *
 * @author Andrew Khokhlov on 04/04/2024
 */
internal interface ConfigStorage {

    /** Get flow of [BlockchainSdkConfig] */
    fun get(): Flow<BlockchainSdkConfig>

    /** Store [BlockchainSdkConfig] */
    suspend fun store(config: BlockchainSdkConfig)
}
