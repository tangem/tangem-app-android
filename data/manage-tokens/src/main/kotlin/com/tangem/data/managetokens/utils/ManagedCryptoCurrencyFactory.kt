package com.tangem.data.managetokens.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.compatibility.applyL2Compatibility
import com.tangem.blockchainsdk.compatibility.getL2CompatibilityTokenComparison
import com.tangem.blockchainsdk.compatibility.l2BlockchainsList
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.blockchainsdk.utils.toCoinId
import com.tangem.data.common.currency.getCoinId
import com.tangem.data.common.currency.getNetwork
import com.tangem.data.common.currency.getTokenId
import com.tangem.datasource.api.tangemTech.models.CoinsResponse
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.local.config.testnet.models.TestnetTokensConfig
import com.tangem.domain.common.DerivationStyleProvider
import com.tangem.domain.common.extensions.canHandleToken
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency.SourceNetwork
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.tokens.model.Network

internal class ManagedCryptoCurrencyFactory(
    private val excludedBlockchains: ExcludedBlockchains,
) {

    fun create(
        coinsResponse: CoinsResponse,
        tokensResponse: UserTokensResponse?,
        scanResponse: ScanResponse?,
    ): List<ManagedCryptoCurrency> {
        return coinsResponse.coins.mapNotNull { coin ->
            createToken(coin, tokensResponse, coinsResponse.imageHost, scanResponse)
        }
    }

    fun createWithCustomTokens(
        coinsResponse: CoinsResponse,
        tokensResponse: UserTokensResponse,
        scanResponse: ScanResponse,
    ): List<ManagedCryptoCurrency> {
        val customTokens = createCustomTokens(tokensResponse, scanResponse)
        val tokens = create(coinsResponse, tokensResponse, scanResponse)

        return customTokens + tokens
    }

    fun createTestnetWithCustomTokens(
        testnetTokensConfig: TestnetTokensConfig,
        tokensResponse: UserTokensResponse?,
        scanResponse: ScanResponse,
    ): List<ManagedCryptoCurrency> {
        val customTokens = tokensResponse
            ?.let { createCustomTokens(it, scanResponse) }
            ?: emptyList()
        val testnetTokens = testnetTokensConfig.tokens.map { testnetToken ->
            ManagedCryptoCurrency.Token(
                id = ManagedCryptoCurrency.ID(testnetToken.id),
                name = testnetToken.name,
                symbol = testnetToken.symbol,
                iconUrl = getIconUrl(testnetToken.id),
                availableNetworks = testnetToken.networks?.mapNotNull { network ->
                    createSource(
                        networkId = network.id,
                        contractAddress = network.address,
                        decimals = network.decimalCount ?: return@mapNotNull null,
                        scanResponse = scanResponse,
                    )
                } ?: emptyList(),
                addedIn = findAddedInNetworks(testnetToken.id, tokensResponse, scanResponse),
            )
        }

        return customTokens + testnetTokens
    }

    private fun createCustomTokens(
        tokensResponse: UserTokensResponse,
        scanResponse: ScanResponse,
    ): List<ManagedCryptoCurrency> = tokensResponse.tokens
        .mapNotNull { token ->
            maybeCreateCustomToken(token, scanResponse)
        }

    private fun maybeCreateCustomToken(
        token: UserTokensResponse.Token,
        scanResponse: ScanResponse,
    ): ManagedCryptoCurrency? {
        val blockchain = Blockchain.fromNetworkId(token.networkId)
            ?.takeUnless { it in excludedBlockchains }
            ?: return null

        if (!checkIsCustomToken(token, blockchain, scanResponse.derivationStyleProvider)) {
            return null
        }

        val network = getNetwork(
            blockchain = blockchain,
            extraDerivationPath = token.derivationPath,
            scanResponse = scanResponse,
            excludedBlockchains = excludedBlockchains,
        ) ?: return null
        val contractAddress = token.contractAddress

        return if (contractAddress.isNullOrBlank()) {
            ManagedCryptoCurrency.Custom.Coin(
                currencyId = getCoinId(network, blockchain.toCoinId()),
                name = token.name,
                symbol = token.symbol,
                iconUrl = getIconUrl(blockchain.id),
                network = network,
            )
        } else {
            ManagedCryptoCurrency.Custom.Token(
                currencyId = getTokenId(network, token.id, contractAddress),
                name = token.name,
                symbol = token.symbol,
                iconUrl = token.id?.let { getIconUrl(it) },
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
        scanResponse: ScanResponse?,
    ): ManagedCryptoCurrency? {
        if (coinResponse.networks.isEmpty() || !coinResponse.active) return null

        val availableNetworks = coinResponse.networks
            .applyL2Compatibility(coinResponse.id)
            .mapNotNull { network ->
                createSource(
                    networkId = network.networkId,
                    contractAddress = network.contractAddress,
                    decimals = network.decimalCount?.toInt(),
                    scanResponse = scanResponse,
                )
            }
            .ifEmpty { return null }

        return ManagedCryptoCurrency.Token(
            id = ManagedCryptoCurrency.ID(coinResponse.id),
            name = coinResponse.name,
            symbol = coinResponse.symbol,
            iconUrl = getIconUrl(coinResponse.id, imageHost),
            availableNetworks = availableNetworks,
            addedIn = findAddedInNetworks(coinResponse.id, tokensResponse, scanResponse),
        )
    }

    private fun createSource(
        networkId: String,
        contractAddress: String?,
        decimals: Int?,
        scanResponse: ScanResponse?,
        extraDerivationPath: String? = null,
    ): SourceNetwork? {
        val blockchain = Blockchain.fromNetworkId(networkId)
            ?.takeUnless { it in excludedBlockchains }
            ?: return null

        val network = getNetwork(
            blockchain,
            extraDerivationPath,
            scanResponse?.derivationStyleProvider,
            excludedBlockchains,
            canHandleTokens = scanResponse?.let {
                it.card.canHandleToken(blockchain, it.cardTypesResolver, excludedBlockchains)
            } ?: true,
        ) ?: return null

        return when {
            contractAddress.isNullOrBlank() -> SourceNetwork.Main(
                network = network,
                decimals = blockchain.decimals(),
                isL2Network = l2BlockchainsList.contains(blockchain),
            )
            network.canHandleTokens -> SourceNetwork.Default(
                network = network,
                decimals = decimals ?: return null,
                contractAddress = contractAddress,
            )
            else -> null
        }
    }

    private fun findAddedInNetworks(
        currencyId: String,
        tokensResponse: UserTokensResponse?,
        scanResponse: ScanResponse?,
    ): Set<Network> {
        if (tokensResponse == null) return emptySet()

        return tokensResponse.tokens
            .filter { getL2CompatibilityTokenComparison(it, currencyId) }
            .mapNotNullTo(mutableSetOf()) { token ->
                val blockchain = Blockchain.fromNetworkId(token.networkId)

                if (blockchain != null && blockchain !in excludedBlockchains) {
                    getNetwork(
                        blockchain,
                        token.derivationPath,
                        scanResponse?.derivationStyleProvider,
                        excludedBlockchains,
                        canHandleTokens = scanResponse?.let {
                            it.card.canHandleToken(blockchain, it.cardTypesResolver, excludedBlockchains)
                        } ?: true,
                    )
                } else {
                    null
                }
            }
    }

    private fun getIconUrl(id: String, imageHost: String? = DEFAULT_IMAGE_HOST): String {
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