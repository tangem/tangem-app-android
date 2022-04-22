package com.tangem.domain.features.addCustomToken

import com.tangem.common.services.Result
import com.tangem.network.api.tangemTech.CoinsResponse
import com.tangem.network.api.tangemTech.TangemTechService

/**
[REDACTED_AUTHOR]
 */
class AddCustomTokenService(
    private val tangemTechService: TangemTechService
) {

    suspend fun findToken(
        contractAddress: String,
        networkId: String? = null,
        active: Boolean? = null,
    ): Result<List<CoinsResponse.Coin>> {
        val result = tangemTechService.coins(contractAddress, networkId, active)
        return when (result) {
            is Result.Success -> {
                var coinsList = mutableListOf<CoinsResponse.Coin>()
                result.data.coins.forEach { coin ->
                    val networksWithTheSameAddress = coin.networks
                        .filter { it.contractAddress != null || it.decimalCount != null }
                        .filter { it.contractAddress == contractAddress }
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
}