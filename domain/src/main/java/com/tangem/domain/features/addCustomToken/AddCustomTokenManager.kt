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
    ): List<Coins.CheckAddressResponse.Token> {
        val result = tangemTechService.coins.checkAddress(contractAddress, networkId)
        return when (result) {
            is Result.Success -> {
                val resultTokens = result.data.tokens
                val newTokensList = mutableListOf<Coins.CheckAddressResponse.Token>()
                resultTokens.forEach {
                    val contractsWithTheSameAddress = it.contracts.filter { it.address == contractAddress }
                    if (contractsWithTheSameAddress.isNotEmpty()) {
                        val newToken = it.copy(contracts = contractsWithTheSameAddress)
                        newTokensList.add(newToken)
                    }
                }
                when {
                    // https://tangem.slack.com/archives/GMXC6PP71/p1649672562078679
                    newTokensList.size > 1 -> listOf(newTokensList[0])
                    else -> newTokensList
                }
            }
            is Result.Failure -> emptyList()
        }
    }

    suspend fun tokens(): List<Coins.TokensResponse.Token> {
        val result = tangemTechService.coins.tokens()
        return when (result) {
            is Result.Success -> {
                val currencies = result.data.tokens
                currencies.filter {
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