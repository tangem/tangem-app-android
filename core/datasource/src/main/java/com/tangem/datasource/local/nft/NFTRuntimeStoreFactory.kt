package com.tangem.datasource.local.nft

import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.domain.models.network.Network
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NFTRuntimeStoreFactory @Inject constructor() {

    fun provide(network: Network): NFTRuntimeStore = DefaultNFTRuntimeStore(
        network = network,
        collectionsRuntimeStore = RuntimeSharedStore(),
        pricesRuntimeStore = RuntimeSharedStore(),
    )
}