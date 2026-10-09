package com.tangem.domain.dynamicaddresses

import com.tangem.domain.dynamicaddresses.repository.DynamicAddressesRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId

class GetUsedDynamicAddressesUseCase(
    private val dynamicAddressesRepository: DynamicAddressesRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, network: Network): List<String> {
        return dynamicAddressesRepository.getUsedAddresses(userWalletId, network)
    }
}