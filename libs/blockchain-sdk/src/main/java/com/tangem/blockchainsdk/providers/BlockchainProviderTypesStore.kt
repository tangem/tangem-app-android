package com.tangem.blockchainsdk.providers

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.datasource.local.datastore.RuntimeStateStore
import javax.inject.Inject
import javax.inject.Singleton

internal typealias BlockchainProviderTypes = Map<Blockchain, List<ProviderType>>

/**
 * Blockchain provider types store
 *
 * @author Andrew Khokhlov on 28/12/2024
 */
@Singleton
class BlockchainProviderTypesStore @Inject constructor() :
    RuntimeStateStore<BlockchainProviderTypes> by RuntimeStateStore(emptyMap())
