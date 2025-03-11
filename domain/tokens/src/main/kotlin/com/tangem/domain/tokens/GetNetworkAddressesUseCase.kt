package com.tangem.domain.tokens

import com.tangem.domain.tokens.model.CryptoCurrencyAddress
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.wallets.models.UserWalletId

class GetNetworkAddressesUseCase(
    internal val networksRepository: NetworksRepository,
) {

    suspend fun invokeSync(userWalletId: UserWalletId, network: Network): List<CryptoCurrencyAddress> {
        return networksRepository.getNetworkAddresses(userWalletId, network)
    }
}