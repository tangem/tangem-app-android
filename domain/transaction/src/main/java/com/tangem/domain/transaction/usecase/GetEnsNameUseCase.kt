package com.tangem.domain.transaction.usecase

import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.WalletAddressServiceRepository
import com.tangem.domain.walletmanager.WalletManagersFacade

class GetEnsNameUseCase(
    private val walletManagersFacade: WalletManagersFacade,
    private val walletAddressServiceRepository: WalletAddressServiceRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, network: Network, address: String): String? {
        if (network.nameResolvingType != Network.NameResolvingType.ENS) {
            return null
        }

        val addresses = walletManagersFacade.getAddresses(userWalletId, network)

        val isOwnAddress = addresses.any { it.value == address }
        if (!isOwnAddress) {
            return null
        }

        return walletAddressServiceRepository.getEns(
            userWalletId = userWalletId,
            network = network,
            address = address,
        )
    }
}