package com.tangem.data.common.currency

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.blockchainsdk.utils.toCoinId
import com.tangem.data.common.network.NetworkFactory
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.scan.ScanResponse
import timber.log.Timber
import com.tangem.blockchain.common.Token as SdkToken

class ResponseCryptoCurrenciesFactory(excludedBlockchains: ExcludedBlockchains) {

    private val networkFactory by lazy(LazyThreadSafetyMode.NONE) { NetworkFactory(excludedBlockchains) }

    fun createCurrency(currencyId: String, response: UserTokensResponse, scanResponse: ScanResponse): CryptoCurrency {
        return response.tokens
            .asSequence()
            .mapNotNull { createCurrency(it, scanResponse) }
            .first { it.id.value == currencyId }
    }

    fun createCurrencies(response: UserTokensResponse, scanResponse: ScanResponse): List<CryptoCurrency> {
        return response.tokens
            .asSequence()
            .mapNotNull { createCurrency(it, scanResponse) }
            .distinctBy { it.id }
            .toList()
    }

    fun createCurrencies(tokens: List<UserTokensResponse.Token>, scanResponse: ScanResponse): List<CryptoCurrency> {
        return tokens
            .asSequence()
            .mapNotNull { createCurrency(it, scanResponse) }
            .distinctBy(CryptoCurrency::id)
            .toList()
    }

    fun createCurrency(responseToken: UserTokensResponse.Token, scanResponse: ScanResponse): CryptoCurrency? {
        var blockchain = Blockchain.fromNetworkId(responseToken.networkId)
        if (blockchain == null || blockchain == Blockchain.Unknown) {
            Timber.e("Unable to find a blockchain with the network ID: ${responseToken.networkId}")
            return null
        }

        if (scanResponse.cardTypesResolver.isTestCard()) {
            blockchain = blockchain.getTestnetVersion() ?: blockchain
        }

        val sdkToken = createSdkToken(responseToken)
        return if (sdkToken == null) {
            createCoin(blockchain, responseToken, scanResponse)
        } else {
            createToken(blockchain, sdkToken, responseToken.derivationPath, scanResponse)
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
        scanResponse: ScanResponse,
    ): CryptoCurrency.Coin? {
        val network = networkFactory.create(
            blockchain = blockchain,
            extraDerivationPath = responseToken.derivationPath,
            scanResponse = scanResponse,
        ) ?: return null

        return CryptoCurrency.Coin(
            id = getCoinId(network, blockchain.toCoinId()),
            network = network,
            name = blockchain.getCoinName(),
            symbol = blockchain.getSymbolForCoin(responseToken),
            decimals = responseToken.decimals,
            iconUrl = getCoinIconUrl(blockchain),
            isCustom = isCustomCoin(network),
        )
    }

    private fun Blockchain.getSymbolForCoin(responseToken: UserTokensResponse.Token): String {
        return when (this) {
            // workaround: Dischain was renamed but backend still returns the old name,
            // get name and symbol from enum Blockchain until backend renamed
            // [REDACTED_JIRA]
            Blockchain.Dischain,
            Blockchain.Polygon,
            -> this.currency
            else -> responseToken.symbol
        }
    }

    private fun createToken(
        blockchain: Blockchain,
        sdkToken: Token,
        responseDerivationPath: String?,
        scanResponse: ScanResponse,
    ): CryptoCurrency.Token? {
        val network = networkFactory.create(
            blockchain = blockchain,
            extraDerivationPath = responseDerivationPath,
            scanResponse = scanResponse,
        ) ?: return null

        val id = getTokenId(network, sdkToken)

        return CryptoCurrency.Token(
            id = id,
            network = network,
            name = sdkToken.name,
            symbol = sdkToken.symbol,
            decimals = sdkToken.decimals,
            iconUrl = getTokenIconUrl(blockchain, sdkToken),
            contractAddress = sdkToken.contractAddress,
            isCustom = isCustomToken(id, network),
        )
    }
}