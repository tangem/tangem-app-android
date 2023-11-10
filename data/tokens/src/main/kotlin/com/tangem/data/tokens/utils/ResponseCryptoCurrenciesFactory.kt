package com.tangem.data.tokens.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.common.DerivationStyleProvider
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.domain.common.extensions.toCoinId
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.demo.DemoConfig
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.tokens.model.CryptoCurrency
import timber.log.Timber
import com.tangem.blockchain.common.Token as SdkToken

internal class ResponseCryptoCurrenciesFactory(private val demoConfig: DemoConfig) {

    fun createCurrency(
        currencyId: CryptoCurrency.ID,
        response: UserTokensResponse,
        scanResponse: ScanResponse,
        derivationPath: String?,
    ): CryptoCurrency {
        val responseTokenId = currencyId.rawCurrencyId
        val networkId = Blockchain.fromId(currencyId.rawNetworkId).toNetworkId()

        val token = requireNotNull(
            value = response.tokens
                .find { it.id == responseTokenId && it.networkId == networkId && it.derivationPath == derivationPath },
            lazyMessage = { "Unable find a token with provided TokenID($responseTokenId) and NetworkID($networkId)" },
        )

        return requireNotNull(createCurrency(token, scanResponse)) {
            "Unable to create a currency with provided ID: $currencyId"
        }
    }

    fun createCurrencies(response: UserTokensResponse, scanResponse: ScanResponse): List<CryptoCurrency> {
        return response.tokens
            .asSequence()
            .mapNotNull { createCurrency(it, scanResponse) }
            .distinctBy { it.id }
            .toList()
    }

    fun createCurrency(responseToken: UserTokensResponse.Token, scanResponse: ScanResponse): CryptoCurrency? {
        var blockchain = Blockchain.fromNetworkId(responseToken.networkId)
        if (blockchain == null || blockchain == Blockchain.Unknown) {
            Timber.e("Unable to find a blockchain with the network ID: ${responseToken.networkId}")
            return null
        }

        val cardDerivationStyleProvider = scanResponse.derivationStyleProvider
        val card = scanResponse.card

        if (demoConfig.isDemoCardId(card.cardId)) {
            blockchain = blockchain.getTestnetVersion() ?: blockchain
        }

        val sdkToken = createSdkToken(responseToken)
        return if (sdkToken == null) {
            createCoin(blockchain, responseToken, cardDerivationStyleProvider)
        } else {
            createToken(blockchain, sdkToken, responseToken.derivationPath, cardDerivationStyleProvider)
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

    private fun createCoin(
        blockchain: Blockchain,
        responseToken: UserTokensResponse.Token,
        derivationStyleProvider: DerivationStyleProvider,
    ): CryptoCurrency.Coin? {
        val network = getNetwork(blockchain, responseToken.derivationPath, derivationStyleProvider) ?: return null

        return CryptoCurrency.Coin(
            id = getCoinId(network, blockchain.toCoinId()),
            network = network,
            networkId = blockchain.toNetworkId(),
            name = responseToken.name,
            symbol = responseToken.symbol,
            decimals = responseToken.decimals,
            iconUrl = getCoinIconUrl(blockchain),
            isCustom = isCustomCoin(network),
        )
    }

    private fun createToken(
        blockchain: Blockchain,
        sdkToken: Token,
        responseDerivationPath: String?,
        derivationStyleProvider: DerivationStyleProvider,
    ): CryptoCurrency.Token? {
        val network = getNetwork(blockchain, responseDerivationPath, derivationStyleProvider)
            ?: return null
        val id = getTokenId(network, sdkToken)

        return CryptoCurrency.Token(
            id = id,
            network = network,
            networkId = blockchain.toNetworkId(),
            name = sdkToken.name,
            symbol = sdkToken.symbol,
            decimals = sdkToken.decimals,
            iconUrl = getTokenIconUrl(blockchain, sdkToken),
            contractAddress = sdkToken.contractAddress,
            isCustom = isCustomToken(id, network),
        )
    }
}
