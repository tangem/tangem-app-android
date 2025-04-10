package com.tangem.domain.nft

import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.wallets.models.UserWalletId

class GetNFTNetworkStatusUseCase(
    private val networksRepository: NetworksRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, network: Network): NetworkStatus? = networksRepository
        .getNetworkStatusesSync(userWalletId, setOf(network), false)
        .firstOrNull { it.network == network }
}