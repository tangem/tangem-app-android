package com.tangem.domain.features.addCustomToken

import com.tangem.common.services.Result
import com.tangem.network.api.tangemTech.Coins
import com.tangem.network.api.tangemTech.TangemTechService
import com.tangem.network.common.AddHeaderInterceptor

/**
[REDACTED_AUTHOR]
 */
class AddCustomTokenManager(
    private val tangemTechService: TangemTechService
) {

    suspend fun checkAddress(
        contractAddress: String,
        networkId: String? = null
    ): Result<List<Coins.CheckAddressResponse.Token>> {
        val result = tangemTechService.coins.checkAddress(contractAddress, networkId)
        return when (result) {
            is Result.Success -> {
                val resultTokens = result.data.tokens
                var tokensList = mutableListOf<Coins.CheckAddressResponse.Token>()
                resultTokens.forEach { token ->
                    val contractsWithTheSameAddress = token.contracts
                        .filter { it.address == contractAddress }
                        .filter { it.decimalCount != null }
                    if (contractsWithTheSameAddress.isNotEmpty()) {
                        val newToken = token.copy(contracts = contractsWithTheSameAddress)
                        tokensList.add(newToken)
                    }
                }
                if (tokensList.size > 1) {
                    // https://tangem.slack.com/archives/GMXC6PP71/p1649672562078679
                    tokensList = mutableListOf(tokensList[0])
                }
                Result.Success(tokensList)
            }
            is Result.Failure -> result
        }
    }

    suspend fun tokens(): List<Coins.TokensResponse.Token> {
        return when (val result = tangemTechService.coins.tokens()) {
            is Result.Success -> {
                val tokens = result.data.tokens
                tokens.filter {
                    it.contracts.isNullOrEmpty()
                }
            }
            is Result.Failure -> emptyList()
        }
    }

    fun attachAuthKey(authKey: String) {
        tangemTechService.addHeaderInterceptors(listOf(
            CardPublicKeyHttpInterceptor(authKey),
        ))
    }
}

private class CardPublicKeyHttpInterceptor(cardPublicKeyHex: String) : AddHeaderInterceptor(mapOf(
    "card_public_key" to cardPublicKeyHex,
))