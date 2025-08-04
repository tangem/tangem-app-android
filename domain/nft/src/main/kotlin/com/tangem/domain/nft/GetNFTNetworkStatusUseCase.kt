package com.tangem.domain.nft

import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.networks.single.SingleNetworkStatusProducer
import com.tangem.domain.networks.single.SingleNetworkStatusSupplier
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.firstOrNull

class GetNFTNetworkStatusUseCase(
    private val singleNetworkStatusSupplier: SingleNetworkStatusSupplier,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, network: Network): NetworkStatus? {
        return singleNetworkStatusSupplier(
            params = SingleNetworkStatusProducer.Params(userWalletId = userWalletId, network = network),
        )
            .firstOrNull()
    }
}