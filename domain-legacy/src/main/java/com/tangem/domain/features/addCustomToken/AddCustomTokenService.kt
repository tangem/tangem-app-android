package com.tangem.domain.features.addCustomToken

import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.CoinsResponse
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

/**
 * Created by Anton Zhilenkov on 02/04/2022.
 */
class AddCustomTokenService(
    private val tangemTechApi: TangemTechApi,
    private val dispatchers: CoroutineDispatcherProvider,
    private val supportedTokenNetworkIds: List<String>,
) {

    suspend fun findToken(
        contractAddress: String,
        networkId: String? = null,
        active: Boolean? = null,
    ): List<CoinsResponse.Coin> = withContext(dispatchers.io) {
        runCatching {
            tangemTechApi.getCoins(
                contractAddress = contractAddress,
                networkIds = selectNetworksForSearch(networkId),
                active = active,
            )
        }
            .onSuccess { response ->
                var coinsList = mutableListOf<CoinsResponse.Coin>()
                response.coins.forEach { coin ->
                    val networksWithTheSameAddress = coin.networks
                        .filter { it.contractAddress != null || it.decimalCount != null }
                        .filter { it.contractAddress?.equals(contractAddress, ignoreCase = true) == true }
                        .filter { supportedTokenNetworkIds.contains(it.networkId) }
                    if (networksWithTheSameAddress.isNotEmpty()) {
                        val newToken = coin.copy(networks = networksWithTheSameAddress)
                        coinsList.add(newToken)
                    }
                }
                if (coinsList.size > 1) {
                    // https://tangem.slack.com/archives/GMXC6PP71/p1649672562078679
                    coinsList = mutableListOf(coinsList[0])
                }
                return@withContext coinsList
            }
            .onFailure {
                return@withContext emptyList()
            }

        error("Unreachable code because runCatching must return result")
    }

    private fun selectNetworksForSearch(networkId: String?): String {
        return networkId ?: supportedTokenNetworkIds.joinToString(",")
    }
}
