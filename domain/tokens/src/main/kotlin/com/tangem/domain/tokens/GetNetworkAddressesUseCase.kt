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

    operator fun invoke(userWalletId: UserWalletId, network: Network): Flow<List<String>> =
        networksRepository.getNetworkStatusesUpdates(userWalletId, setOf(network))
            .map { networkStatuses ->
                networkStatuses.filter { it.network.id == network.id }
                    .map { networkStatus ->
                        when (val status = networkStatus.value) {
                            is NetworkStatus.NoAccount -> status.address.defaultAddress.value
                            is NetworkStatus.Unreachable -> status.address?.defaultAddress?.value.orEmpty()
                            is NetworkStatus.Verified -> status.address.defaultAddress.value
                            else -> ""
                        }
                    }
            }
}
