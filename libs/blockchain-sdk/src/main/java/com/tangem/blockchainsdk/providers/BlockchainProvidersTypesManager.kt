package com.tangem.blockchainsdk.providers

import kotlinx.coroutines.flow.Flow

/** Blockchain providers types manager */
interface BlockchainProvidersTypesManager {

    fun get(): Flow<BlockchainProviderTypes>

    suspend fun update()
}