package com.tangem.domain.tokens

import com.tangem.domain.models.network.CryptoCurrencyAddress
import com.tangem.domain.models.network.Network
import com.tangem.domain.networks.repository.NetworksRepository
import com.tangem.domain.wallets.models.UserWalletId

class GetNetworkAddressesUseCase(
    private val networksRepository: NetworksRepository,
) {

    suspend fun invokeSync(userWalletId: UserWalletId, networkRawId: Network.RawID): List<CryptoCurrencyAddress> {
        return networksRepository.getNetworkAddresses(userWalletId, networkRawId)
    }

    suspend fun invokeSync(userWalletId: UserWalletId, network: Network): List<CryptoCurrencyAddress> {
        return networksRepository.getNetworkAddresses(userWalletId, network)
    }
}