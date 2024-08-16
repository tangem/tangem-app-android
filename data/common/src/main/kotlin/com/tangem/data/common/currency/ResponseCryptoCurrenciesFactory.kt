package com.tangem.data.common.currency

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.blockchainsdk.utils.toCoinId
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.common.DerivationStyleProvider
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.tokens.model.CryptoCurrency
import timber.log.Timber
import com.tangem.blockchain.common.Token as SdkToken

class ResponseCryptoCurrenciesFactory {

    fun createCurrency(
        currencyId: CryptoCurrency.ID,
        response: UserTokensResponse,
        scanResponse: ScanResponse,
    ): CryptoCurrency {
        return response.tokens
            .asSequence()
            .mapNotNull { createCurrency(it, scanResponse) }
            .first { it.id == currencyId }
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

        if (scanResponse.cardTypesResolver.isTestCard()) {
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
            name = blockchain.getNameForCoin(responseToken),
            symbol = blockchain.getSymbolForCoin(responseToken),
            decimals = responseToken.decimals,
            iconUrl = getCoinIconUrl(blockchain),
            isCustom = isCustomCoin(network),
        )
    }

    private fun Blockchain.getNameForCoin(responseToken: UserTokensResponse.Token): String {
        return when (this) {
            // workaround: for Blockchains full name different than backend name,
            // get name and symbol from enum Blockchain until backend renamed
// [REDACTED_JIRA]
            Blockchain.Dischain,
            Blockchain.Arbitrum,
            Blockchain.ArbitrumTestnet,
            Blockchain.Aurora,
            Blockchain.AuroraTestnet,
            Blockchain.Manta,
            Blockchain.MantaTestnet,
            Blockchain.ZkSyncEra,
            Blockchain.ZkSyncEraTestnet,
            Blockchain.PolygonZkEVM,
            Blockchain.PolygonZkEVMTestnet,
            Blockchain.Base,
            Blockchain.BaseTestnet,
            Blockchain.Telos,
            Blockchain.Cronos,
            Blockchain.TON,
            Blockchain.Cyber,
            -> this.fullName
            else -> responseToken.name
        }
    }

    private fun Blockchain.getSymbolForCoin(responseToken: UserTokensResponse.Token): String {
        return when (this) {
            // workaround: Dischain was renamed but backend still returns the old name,
            // get name and symbol from enum Blockchain until backend renamed
// [REDACTED_JIRA]
            Blockchain.Dischain,
            -> this.currency
            else -> responseToken.symbol
        }
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
            name = sdkToken.name,
            symbol = sdkToken.symbol,
            decimals = sdkToken.decimals,
            iconUrl = getTokenIconUrl(blockchain, sdkToken),
            contractAddress = sdkToken.contractAddress,
            isCustom = isCustomToken(id, network),
        )
    }
}
