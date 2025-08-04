package com.tangem.data.common.currency

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.datasource.local.token.UserTokensResponseStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.card.common.TapWorkarounds.isTestCard
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.demo.models.DemoConfig
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isMultiCurrency

/**
 * Default implementation of factory for creating list of [CryptoCurrency] for selected card
 *
 * @property demoConfig              demo config
 * @property excludedBlockchains     excluded blockchains
 * @property userWalletsStore        user wallets store
 * @property userTokensResponseStore user tokens response store
 */
internal class DefaultCardCryptoCurrencyFactory(
    private val demoConfig: DemoConfig,
    private val excludedBlockchains: ExcludedBlockchains,
    private val userWalletsStore: UserWalletsStore,
    private val userTokensResponseStore: UserTokensResponseStore,
    private val responseCryptoCurrenciesFactory: ResponseCryptoCurrenciesFactory,
) : CardCryptoCurrencyFactory {

    private val cryptoCurrencyFactory by lazy { CryptoCurrencyFactory(excludedBlockchains) }

    override suspend fun create(
        userWalletId: UserWalletId,
        networks: Set<Network>,
    ): Map<Network, List<CryptoCurrency>> {
        val userWallet = userWalletsStore.getSyncStrict(key = userWalletId)

        // multi-currency wallet
        if (userWallet !is UserWallet.Cold || userWallet.isMultiCurrency) {
            return getMultiWalletCurrencies(userWallet = userWallet, networks = networks)
        }

        // check if the blockchain of single-currency wallet is the same as network
        val cardNetworkId = userWallet.scanResponse.cardTypesResolver.getBlockchain().toNetworkId()
        val cardNetwork = networks.firstOrNull { it.backendId == cardNetworkId }

        if (cardNetwork == null) return emptyMap()

        // single-currency wallet with token (NODL)
        if (userWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken()) {
            val currencies = createCurrenciesForSingleCurrencyCardWithToken(userWallet)
            return mapOf(cardNetwork to currencies)
        }

        // single-currency wallet
        val primaryCurrency = createPrimaryCurrencyForSingleCurrencyCard(userWallet)
        return mapOf(cardNetwork to listOf(primaryCurrency))
    }

    override suspend fun createByRawId(userWalletId: UserWalletId, network: Network.RawID): List<CryptoCurrency> {
        val userWallet = userWalletsStore.getSyncStrict(key = userWalletId)

        val blockchain = network.toBlockchain()

        // multi-currency wallet
        if (userWallet.isMultiCurrency || userWallet !is UserWallet.Cold) {
            return getMultiWalletCurrenciesByRawId(
                userWallet = userWallet,
                rawIds = setOf(network),
            )[network].orEmpty()
        }

        // check if the blockchain of single-currency wallet is the same as network
        val cardBlockchain = userWallet.scanResponse.cardTypesResolver.getBlockchain()
        if (cardBlockchain != blockchain) return emptyList()

        // single-currency wallet with token (NODL)
        if (userWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken()) {
            return createCurrenciesForSingleCurrencyCardWithToken(userWallet)
        }

        // single-currency wallet
        return createPrimaryCurrencyForSingleCurrencyCard(userWallet).let(::listOf)
    }

    override suspend fun createCurrenciesForMultiCurrencyCard(
        userWallet: UserWallet,
        networks: Set<Network>,
    ): Map<Network, List<CryptoCurrency>> {
        require(userWallet.isMultiCurrency) { "It isn't multi-currency wallet" }

        return getMultiWalletCurrencies(userWallet = userWallet, networks = networks)
    }

    override fun createDefaultCoinsForMultiCurrencyWallet(userWallet: UserWallet): List<CryptoCurrency.Coin> {
        require(userWallet.isMultiCurrency) { "It isn't multi-currency wallet" }

        val blockchains = when (userWallet) {
            is UserWallet.Cold -> {
                val card = userWallet.scanResponse.card

                var blockchainsInternal = if (demoConfig.isDemoCardId(card.cardId)) {
                    demoConfig.demoBlockchains
                } else {
                    listOf(Blockchain.Bitcoin, Blockchain.Ethereum)
                }

                if (card.isTestCard) {
                    blockchainsInternal = blockchainsInternal.mapNotNull { it.getTestnetVersion() }
                }

                blockchainsInternal
            }

            is UserWallet.Hot -> listOf(Blockchain.Bitcoin, Blockchain.Ethereum)
        }

        return blockchains.mapNotNull {
            cryptoCurrencyFactory.createCoin(
                blockchain = it,
                extraDerivationPath = null,
                userWallet = userWallet,
            )
        }
    }

    override fun createPrimaryCurrencyForSingleCurrencyCard(userWallet: UserWallet.Cold): CryptoCurrency {
        require(userWallet.scanResponse.cardTypesResolver.isSingleWallet()) {
            "It isn't single-currency wallet"
        }

        return with(getSingleWalletCurrencies(userWallet)) {
            primaryToken ?: coin
        }
    }

    override fun createCurrenciesForSingleCurrencyCardWithToken(userWallet: UserWallet.Cold): List<CryptoCurrency> {
        require(userWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken()) {
            "It isn't single-currency wallet with token"
        }

        return with(getSingleWalletCurrencies(userWallet)) {
            listOfNotNull(coin, primaryToken)
        }
    }

    private suspend fun getMultiWalletCurrencies(
        userWallet: UserWallet,
        networks: Set<Network>,
    ): Map<Network, List<CryptoCurrency>> {
        val response = userTokensResponseStore.getSyncOrNull(userWalletId = userWallet.walletId)
            ?: return emptyMap()

        val existingNetworkWithCurrencies = responseCryptoCurrenciesFactory.createCurrencies(
            tokens = response.tokens.filter { token ->
                networks.any { it.backendId == token.networkId && it.derivationPath.value == token.derivationPath }
            },
            userWallet = userWallet,
        )
            .groupBy(CryptoCurrency::network)

        return networks.associateWith { emptyList<CryptoCurrency>() } + existingNetworkWithCurrencies
    }

    private suspend fun getMultiWalletCurrenciesByRawId(
        userWallet: UserWallet,
        rawIds: Set<Network.RawID>,
    ): Map<Network.RawID, List<CryptoCurrency>> {
        val response = userTokensResponseStore.getSyncOrNull(userWalletId = userWallet.walletId)
            ?: return emptyMap()

        val networkIds = rawIds.map { it.toBlockchain().toNetworkId() }

        return responseCryptoCurrenciesFactory.createCurrencies(
            tokens = response.tokens.filter { token -> token.networkId in networkIds },
            userWallet = userWallet,
        )
            .groupBy { it.network.id.rawId }
    }

    private fun getSingleWalletCurrencies(userWallet: UserWallet.Cold): SingleWalletCurrencies {
        val resolver = userWallet.cardTypesResolver
        val blockchain = resolver.getBlockchain()

        val coin = cryptoCurrencyFactory.createCoin(
            blockchain = blockchain,
            extraDerivationPath = null,
            userWallet = userWallet,
        )

        requireNotNull(coin) { "Coin for the single currency card cannot be null" }

        val primaryToken = resolver.getPrimaryToken()?.let { token ->
            cryptoCurrencyFactory.createToken(
                sdkToken = token,
                blockchain = blockchain,
                extraDerivationPath = null,
                userWallet = userWallet,
            )
        }

        return SingleWalletCurrencies(coin = coin, primaryToken = primaryToken)
    }

    private data class SingleWalletCurrencies(
        val coin: CryptoCurrency,
        val primaryToken: CryptoCurrency?,
    )
}