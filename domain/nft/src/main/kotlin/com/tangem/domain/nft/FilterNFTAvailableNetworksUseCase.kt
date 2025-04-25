package com.tangem.domain.nft

import com.tangem.domain.tokens.model.Network
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

class FilterNFTAvailableNetworksUseCase(
    private val dispatchers: CoroutineDispatcherProvider,
) {
    suspend operator fun invoke(networks: List<Network>, searchQuery: String): List<Network> =
        withContext(dispatchers.default) {
            networks.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                    it.currencySymbol.contains(searchQuery, ignoreCase = true)
            }
        }
}