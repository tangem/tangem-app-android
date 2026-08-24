package com.tangem.data.jointaccount

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.jointaccount.repository.JointAccountSupportedNetworksRepository
import com.tangem.domain.models.network.Network
import javax.inject.Inject

internal class DefaultJointAccountSupportedNetworksRepository @Inject constructor() :
    JointAccountSupportedNetworksRepository {

    override fun getSupportedNetworks(): List<Network.RawID> = SUPPORTED_NETWORKS

    private companion object {

        val SUPPORTED_NETWORKS: List<Network.RawID> = listOf(
            Blockchain.Ethereum,
            Blockchain.Avalanche,
            Blockchain.Arbitrum,
            Blockchain.Optimism,
            Blockchain.Base,
            Blockchain.BSC,
            Blockchain.Polygon,
        ).map { blockchain ->
            Network.RawID(value = blockchain.toNetworkId())
        }
    }
}