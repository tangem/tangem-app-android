package com.tangem.data.nft.store

import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.domain.models.network.Network
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class NFTRuntimeStoreFactory @Inject constructor() {

    fun provide(network: Network): NFTRuntimeStore = DefaultNFTRuntimeStore(
        network = network,
        collectionsRuntimeStore = RuntimeSharedStore(),
        pricesRuntimeStore = RuntimeSharedStore(),
    )
}