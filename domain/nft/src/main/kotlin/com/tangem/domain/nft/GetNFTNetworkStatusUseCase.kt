package com.tangem.domain.nft

import com.tangem.domain.networks.single.SingleNetworkStatusProducer
import com.tangem.domain.networks.single.SingleNetworkStatusSupplier
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.wallets.models.UserWalletId
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