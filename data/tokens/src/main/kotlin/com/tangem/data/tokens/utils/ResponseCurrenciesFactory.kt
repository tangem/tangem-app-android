package com.tangem.data.tokens.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.domain.demo.DemoConfig
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.tokens.model.CryptoCurrency
import timber.log.Timber
import com.tangem.blockchain.common.Token as SdkToken

internal class ResponseCurrenciesFactory(private val demoConfig: DemoConfig) {

    fun createCurrency(currencyId: CryptoCurrency.ID, response: UserTokensResponse, card: CardDTO): CryptoCurrency {
        val responseTokenId = getTokenIdString(currencyId)

        val token = requireNotNull(response.tokens.firstOrNull { it.id == responseTokenId }) {
            "Unable find a token with provided ID: $responseTokenId"
        }

        return requireNotNull(createCurrency(token, card)) {
            "Unable to create a currency with provided ID: $currencyId"
        }
    }

    fun createCurrencies(response: UserTokensResponse, card: CardDTO): List<CryptoCurrency> {
        return response.tokens.mapNotNull { createCurrency(it, card) }
    }

    private fun createCurrency(responseToken: UserTokensResponse.Token, card: CardDTO): CryptoCurrency? {
        var blockchain = Blockchain.fromNetworkId(responseToken.networkId)
        if (blockchain == null || blockchain == Blockchain.Unknown) {
            Timber.e("Unable to find a blockchain with the network ID: ${responseToken.networkId}")
            return null
        }

        if (demoConfig.isDemoCardId(card.cardId)) {
            blockchain = blockchain.getTestnetVersion() ?: blockchain
        }

        val sdkToken = createSdkToken(responseToken)
        return if (sdkToken == null) {
            createCoin(blockchain, responseToken)
        } else {
            createToken(blockchain, sdkToken, responseToken.derivationPath)
        }
    }

    private fun createSdkToken(token: UserTokensResponse.Token): SdkToken? {
        return token.contractAddress?.let { contractAddress ->
            SdkToken(
                name = token.name,
                symbol = token.symbol,
                contractAddress = contractAddress,
                decimals = token.decimals,
                id = token.id,
            )
        }
    }

    private fun createCoin(blockchain: Blockchain, responseToken: UserTokensResponse.Token): CryptoCurrency.Coin {
        return CryptoCurrency.Coin(
            id = getCoinId(blockchain),
            networkId = getNetworkId(blockchain),
            name = responseToken.name,
            symbol = responseToken.symbol,
            decimals = responseToken.decimals,
            derivationPath = responseToken.derivationPath,
            iconUrl = getCoinIconUrl(blockchain),
        )
    }

    private fun createToken(blockchain: Blockchain, sdkToken: Token, derivationPath: String?): CryptoCurrency.Token {
        val id = getTokenId(blockchain, sdkToken)

        return CryptoCurrency.Token(
            id = id,
            networkId = getNetworkId(blockchain),
            name = sdkToken.name,
            symbol = sdkToken.symbol,
            decimals = sdkToken.decimals,
            derivationPath = derivationPath,
            iconUrl = getTokenIconUrl(blockchain, sdkToken),
            contractAddress = sdkToken.contractAddress,
            isCustom = isCustomToken(id),
        )
    }
}