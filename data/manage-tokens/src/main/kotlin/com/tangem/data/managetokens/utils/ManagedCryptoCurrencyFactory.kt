package com.tangem.data.managetokens.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.blockchainsdk.utils.isSupportedInApp
import com.tangem.blockchainsdk.utils.toCoinId
import com.tangem.data.common.currency.getCoinId
import com.tangem.data.common.currency.getNetwork
import com.tangem.data.common.currency.getTokenId
import com.tangem.datasource.api.tangemTech.models.CoinsResponse
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.common.DerivationStyleProvider
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency.SourceNetwork
import com.tangem.domain.tokens.model.Network

internal class ManagedCryptoCurrencyFactory {

    fun create(
        coinsResponse: CoinsResponse,
        tokensResponse: UserTokensResponse?,
        derivationStyleProvider: DerivationStyleProvider?,
    ): List<ManagedCryptoCurrency> {
        return coinsResponse.coins.mapNotNull { coin ->
            createToken(coin, tokensResponse, coinsResponse.imageHost, derivationStyleProvider)
        }
    }

    fun createWithCustomTokens(
        coinsResponse: CoinsResponse,
        tokensResponse: UserTokensResponse,
        derivationStyleProvider: DerivationStyleProvider,
    ): List<ManagedCryptoCurrency> {
        val customTokens = tokensResponse.tokens
            .mapNotNull { token ->
                maybeCreateCustomToken(token, coinsResponse.imageHost, derivationStyleProvider)
            }

        val tokens = create(coinsResponse, tokensResponse, derivationStyleProvider)

        return customTokens + tokens
    }

    private fun maybeCreateCustomToken(
        token: UserTokensResponse.Token,
        imageHost: String?,
        derivationStyleProvider: DerivationStyleProvider,
    ): ManagedCryptoCurrency? {
        val blockchain = Blockchain.fromNetworkId(token.networkId)
            ?.takeIf { it.isSupportedInApp() }
            ?: return null

        if (!checkIsCustomToken(token, blockchain, derivationStyleProvider)) {
            return null
        }

        val network = getNetwork(
            blockchain = blockchain,
            extraDerivationPath = token.derivationPath,
            derivationStyleProvider = derivationStyleProvider,
        ) ?: return null
        val contractAddress = token.contractAddress

        return if (contractAddress.isNullOrBlank()) {
            ManagedCryptoCurrency.Custom.Coin(
                currencyId = getCoinId(network, blockchain.toCoinId()),
                name = token.name,
                symbol = token.symbol,
                iconUrl = getIconUrl(blockchain.id, imageHost),
                network = network,
            )
        } else {
            ManagedCryptoCurrency.Custom.Token(
                currencyId = getTokenId(network, token.id, contractAddress),
                name = token.name,
                symbol = token.symbol,
                iconUrl = token.id?.let { getIconUrl(it, imageHost) },
                contractAddress = contractAddress,
                network = network,
                decimals = token.decimals,
            )
        }
    }

    private fun createToken(
        coinResponse: CoinsResponse.Coin,
        tokensResponse: UserTokensResponse?,
        imageHost: String?,
        derivationStyleProvider: DerivationStyleProvider?,
    ): ManagedCryptoCurrency? {
        if (coinResponse.networks.isEmpty() || !coinResponse.active) return null

        return ManagedCryptoCurrency.Token(
            id = ManagedCryptoCurrency.ID(coinResponse.id),
            name = coinResponse.name,
            symbol = coinResponse.symbol,
            iconUrl = getIconUrl(coinResponse.id, imageHost),
            availableNetworks = coinResponse.networks.mapNotNull { network ->
                createSource(network, derivationStyleProvider)
            },
            addedIn = findAddedInNetworks(coinResponse.id, tokensResponse, derivationStyleProvider),
        )
    }

    private fun createSource(
        networkResponse: CoinsResponse.Coin.Network,
        derivationStyleProvider: DerivationStyleProvider?,
        extraDerivationPath: String? = null,
    ): SourceNetwork? {
        val blockchain = Blockchain.fromNetworkId(networkResponse.networkId)
            ?.takeIf { it.isSupportedInApp() }
            ?: return null

        val network = getNetwork(blockchain, extraDerivationPath, derivationStyleProvider) ?: return null
        val contractAddress = networkResponse.contractAddress

        return if (contractAddress.isNullOrBlank()) {
            SourceNetwork.Main(
                network = network,
                decimals = blockchain.decimals(),
            )
        } else {
            SourceNetwork.Default(
                network = network,
                decimals = requireNotNull(networkResponse.decimalCount?.toInt()),
                contractAddress = contractAddress,
            )
        }
    }

    private fun findAddedInNetworks(
        currencyId: String,
        tokensResponse: UserTokensResponse?,
        derivationStyleProvider: DerivationStyleProvider?,
    ): Set<Network> {
        if (tokensResponse == null) return emptySet()

        return tokensResponse.tokens
            .filter { it.id == currencyId }
            .mapNotNullTo(mutableSetOf()) { token ->
                val blockchain = Blockchain.fromNetworkId(token.networkId)

                if (blockchain != null && blockchain.isSupportedInApp()) {
                    getNetwork(
                        blockchain = blockchain,
                        extraDerivationPath = token.derivationPath,
                        derivationStyleProvider = derivationStyleProvider,
                    )
                } else {
                    null
                }
            }
    }

    private fun getIconUrl(id: String, imageHost: String?): String {
        return "${imageHost ?: DEFAULT_IMAGE_HOST}large/$id.png"
    }

    private fun checkIsCustomToken(
        token: UserTokensResponse.Token,
        blockchain: Blockchain,
        derivationStyleProvider: DerivationStyleProvider,
    ): Boolean = token.id.isNullOrBlank() ||
        checkIsCustomDerivationPath(token.derivationPath, blockchain, derivationStyleProvider)

    private fun checkIsCustomDerivationPath(
        derivationPath: String?,
        blockchain: Blockchain,
        derivationStyleProvider: DerivationStyleProvider,
    ): Boolean = derivationPath != blockchain.derivationPath(derivationStyleProvider.getDerivationStyle())?.rawPath

    private companion object {
        const val DEFAULT_IMAGE_HOST = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/"
    }
}