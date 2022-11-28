package com.tangem.domain.features.addCustomToken

import com.tangem.common.services.Result
import com.tangem.domain.common.extensions.getTokens
import com.tangem.datasource.api.tangemTech.CoinsResponse
import com.tangem.datasource.api.tangemTech.TangemTechService

/**
 * Created by Anton Zhilenkov on 02/04/2022.
 */
class AddCustomTokenService(
    private val tangemTechService: TangemTechService,
    private val supportedTokenNetworkIds: List<String>,
) {

    suspend fun findToken(
        contractAddress: String,
        networkId: String? = null,
        active: Boolean? = null,
    ): Result<List<CoinsResponse.Coin>> {
        val networksIds = selectNetworksForSearch(networkId)
        val result = tangemTechService.getTokens(contractAddress, networksIds, active)
        return when (result) {
            is Result.Success -> {
                var coinsList = mutableListOf<CoinsResponse.Coin>()
                result.data.coins.forEach { coin ->
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
                Result.Success(coinsList)
            }
            is Result.Failure -> result
        }
    }

    private fun selectNetworksForSearch(networkId: String?): String {
        return networkId ?: supportedTokenNetworkIds.joinToString(",")
    }
}
