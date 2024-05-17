package com.tangem.domain.tokens

import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetNetworkAddressesUseCase(
    internal val networksRepository: NetworksRepository,
) {

    operator fun invoke(userWalletId: UserWalletId, network: Network): Flow<String> =
        networksRepository.getNetworkStatusesUpdates(userWalletId, setOf(network))
            .map { networkStatuses ->
                when (val networkStatus = networkStatuses.singleOrNull { it.network.id == network.id }?.value) {
                    is NetworkStatus.NoAccount -> networkStatus.address.defaultAddress.value
                    is NetworkStatus.Unreachable -> networkStatus.address?.defaultAddress?.value.orEmpty()
                    is NetworkStatus.Verified -> networkStatus.address.defaultAddress.value
                    else -> ""
                }
            }
}
