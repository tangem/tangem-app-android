package com.tangem.domain.dynamicaddresses

import com.tangem.domain.dynamicaddresses.model.DynamicAddressesStatus
import com.tangem.domain.dynamicaddresses.repository.DynamicAddressesRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

class GetDynamicAddressesStatusUseCase(
    private val dynamicAddressesRepository: DynamicAddressesRepository,
) {

    operator fun invoke(userWalletId: UserWalletId, network: Network): Flow<DynamicAddressesStatus> {
        return dynamicAddressesRepository.getStatus(userWalletId, network)
    }
}