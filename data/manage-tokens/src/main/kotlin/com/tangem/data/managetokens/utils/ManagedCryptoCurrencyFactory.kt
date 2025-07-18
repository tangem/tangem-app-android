package com.tangem.data.managetokens.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.compatibility.applyL2Compatibility
import com.tangem.blockchainsdk.compatibility.getL2CompatibilityTokenComparison
import com.tangem.blockchainsdk.compatibility.l2BlockchainsList
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.blockchainsdk.utils.toCoinId
import com.tangem.data.common.currency.getCoinId
import com.tangem.data.common.currency.getTokenId
import com.tangem.data.common.network.NetworkFactory
import com.tangem.datasource.api.tangemTech.models.CoinsResponse
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.local.config.testnet.models.TestnetTokensConfig
import com.tangem.domain.common.DerivationStyleProvider
import com.tangem.domain.common.extensions.canHandleToken
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency.SourceNetwork
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.wallets.models.UserWallet
import timber.log.Timber

internal class ManagedCryptoCurrencyFactory(
    private val networkFactory: NetworkFactory,
    private val excludedBlockchains: ExcludedBlockchains,
) {

    fun create(
        coinsResponse: CoinsResponse,
        tokensResponse: UserTokensResponse?,
        userWallet: UserWallet?,
    ): List<ManagedCryptoCurrency> {
        return coinsResponse.coins.mapNotNull { coin ->
            createToken(coin, tokensResponse, coinsResponse.imageHost, userWallet)
        }
    }

    fun createWithCustomTokens(
        coinsResponse: CoinsResponse,
        tokensResponse: UserTokensResponse,
        userWallet: UserWallet,
    ): List<ManagedCryptoCurrency> {
        val customTokens = createCustomTokens(tokensResponse, userWallet)
        val tokens = create(coinsResponse, tokensResponse, userWallet)

        return customTokens + tokens
    }

    fun createTestnetWithCustomTokens(
        testnetTokensConfig: TestnetTokensConfig,
        tokensResponse: UserTokensResponse?,
        userWallet: UserWallet,
    ): List<ManagedCryptoCurrency> {
        val customTokens = tokensResponse
            ?.let { createCustomTokens(it, userWallet) }
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
                        decimals = network.decimalCount,
                        userWallet = userWallet,
                    )
                } ?: emptyList(),
                addedIn = findAddedInNetworks(testnetToken.id, tokensResponse, userWallet),
            )
        }

        return customTokens + testnetTokens
    }

    private fun createCustomTokens(
        tokensResponse: UserTokensResponse,
        userWallet: UserWallet,
    ): List<ManagedCryptoCurrency> = tokensResponse.tokens
        .mapNotNull { token ->
            maybeCreateCustomToken(token, userWallet)
        }

    private fun maybeCreateCustomToken(
        token: UserTokensResponse.Token,
        userWallet: UserWallet,
    ): ManagedCryptoCurrency? {
        val blockchain = Blockchain.fromNetworkId(token.networkId)
            ?.takeUnless { it in excludedBlockchains }
            ?: return null

        if (!checkIsCustomToken(token, blockchain, userWallet.derivationStyleProvider)) {
            return null
        }

        val network = networkFactory.create(
            blockchain = blockchain,
            extraDerivationPath = token.derivationPath,
            userWallet = userWallet,
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
                currencyId = getTokenId(
                    network = network,
                    rawTokenId = token.id?.let { CryptoCurrency.RawID(it) },
                    contractAddress = contractAddress,
                ),
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
        userWallet: UserWallet?,
    ): ManagedCryptoCurrency? {
        if (coinResponse.networks.isEmpty() || !coinResponse.active) return null

        val availableNetworks = coinResponse.networks
            .applyL2Compatibility(coinResponse.id)
            .mapNotNull { network ->
                createSource(
                    networkId = network.networkId,
                    contractAddress = network.contractAddress,
                    decimals = network.decimalCount?.toInt(),
                    userWallet = userWallet,
                )
            }
            .ifEmpty { return null }

        return ManagedCryptoCurrency.Token(
            id = ManagedCryptoCurrency.ID(coinResponse.id),
            name = coinResponse.name,
            symbol = coinResponse.symbol,
            iconUrl = getIconUrl(coinResponse.id, imageHost),
            availableNetworks = availableNetworks,
            addedIn = findAddedInNetworks(coinResponse.id, tokensResponse, userWallet),
        )
    }

    private fun createSource(
        networkId: String,
        contractAddress: String?,
        decimals: Int?,
        userWallet: UserWallet?,
        extraDerivationPath: String? = null,
    ): SourceNetwork? {
        val blockchain = Blockchain.fromNetworkId(networkId)
            ?.takeUnless { it in excludedBlockchains }
            ?: return null

        val network = networkFactory.create(
            blockchain = blockchain,
            extraDerivationPath = extraDerivationPath,
            derivationStyleProvider = userWallet?.derivationStyleProvider,
            canHandleTokens = userWallet?.canHandleToken(blockchain, excludedBlockchains) ?: false,
        ) ?: return null

        return when {
            contractAddress.isNullOrBlank() -> SourceNetwork.Main(
                network = network,
                decimals = blockchain.decimals(),
                isL2Network = l2BlockchainsList.contains(blockchain),
            )
            // use general check from blockchain, check availability for card in place of use
            blockchain.canHandleTokens() -> {
                val formattedContractAddress = blockchain.reformatContractAddress(contractAddress)
                if (formattedContractAddress == null) {
                    Timber.w("Couldn't reformat $contractAddress")
                    return null
                }
                SourceNetwork.Default(
                    network = network,
                    decimals = decimals ?: return null,
                    contractAddress = formattedContractAddress,
                )
            }
            else -> null
        }
    }

    private fun findAddedInNetworks(
        currencyId: String,
        tokensResponse: UserTokensResponse?,
        userWallet: UserWallet?,
    ): Set<Network> {
        if (tokensResponse == null) return emptySet()

        return tokensResponse.tokens
            .filter { getL2CompatibilityTokenComparison(it, currencyId) }
            .mapNotNullTo(mutableSetOf()) { token ->
                val blockchain = Blockchain.fromNetworkId(token.networkId)

                if (blockchain != null && blockchain !in excludedBlockchains) {
                    networkFactory.create(
                        blockchain = blockchain,
                        extraDerivationPath = token.derivationPath,
                        derivationStyleProvider = userWallet?.derivationStyleProvider,
                        canHandleTokens = userWallet?.canHandleToken(blockchain, excludedBlockchains) ?: true,
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