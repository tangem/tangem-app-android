package com.tangem.domain.nft

import com.tangem.domain.models.network.Network
import com.tangem.domain.nft.models.NFTNetworks
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

class FilterNFTAvailableNetworksUseCase(
    private val dispatchers: CoroutineDispatcherProvider,
) {
    suspend operator fun invoke(networks: NFTNetworks, searchQuery: String): NFTNetworks =
        withContext(dispatchers.default) {
            NFTNetworks(
                availableNetworks = networks.availableNetworks.filter(searchQuery),
                unavailableNetworks = networks.unavailableNetworks.filter(searchQuery),
            )
        }

    private fun List<Network>.filter(searchQuery: String) = filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
            it.currencySymbol.contains(searchQuery, ignoreCase = true)
    }
}