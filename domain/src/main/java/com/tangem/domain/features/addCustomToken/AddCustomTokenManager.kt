package com.tangem.domain.features.addCustomToken

import com.tangem.common.services.Result
import com.tangem.network.api.tangemTech.CoinsCheckAddressResponse
import com.tangem.network.api.tangemTech.TangemAuthInterceptor
import com.tangem.network.api.tangemTech.TangemTechService

/**
[REDACTED_AUTHOR]
 */
class AddCustomTokenManager(
    private val tangemTechService: TangemTechService
) {

    suspend fun findContractAddress(
        contractAddress: String,
        networkId: String? = null
    ): List<CoinsCheckAddressResponse.Token> {
        val result = tangemTechService.coinsCheckAddress(contractAddress, networkId)
        return when (result) {
            is Result.Success -> {
                result.data.tokens
            }
            is Result.Failure -> emptyList()
        }
    }

    fun attachAuthKey(authKey: String) {
        tangemTechService.addHeaderInterceptors(listOf(TangemAuthInterceptor(authKey)))
    }
}