package com.tangem.blockchainsdk.providers

import kotlinx.coroutines.flow.StateFlow

/** Blockchain providers types manager */
interface BlockchainProvidersTypesManager {

    fun get(): StateFlow<BlockchainProviderTypes>

    suspend fun update()
}