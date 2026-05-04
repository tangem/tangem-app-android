package com.tangem.domain.assetsdiscovery

import com.tangem.blockchain.assetsdiscovery.AssetsDiscoveryService
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId

interface AssetsDiscoveryFacade {

    suspend fun getAssetsDiscoveryService(userWalletId: UserWalletId, network: Network): AssetsDiscoveryServiceInfo?

    data class AssetsDiscoveryServiceInfo(
        val address: String,
        val service: AssetsDiscoveryService,
    )
}