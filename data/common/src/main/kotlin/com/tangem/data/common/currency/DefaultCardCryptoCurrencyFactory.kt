package com.tangem.data.common.currency

import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.data.common.tokens.getDefaultWalletBlockchains
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.common.tokens.CardCryptoCurrencyFactory
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.demo.models.DemoConfig
import com.tangem.domain.jointaccount.derivation.toNetworkDerivationPath
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.account.OwnerKeyIndex
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
 */
@Suppress("LongParameterList")
internal class DefaultCardCryptoCurrencyFactory(
    private val demoConfig: DemoConfig,
    private val excludedBlockchains: ExcludedBlockchains,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val walletAccountsFetcher: WalletAccountsFetcher,
    private val responseCryptoCurrenciesFactory: ResponseCryptoCurrenciesFactory,
) : CardCryptoCurrencyFactory {

    private val cryptoCurrencyFactory by lazy { CryptoCurrencyFactory(excludedBlockchains) }

    override suspend fun create(
        userWalletId: UserWalletId,
        networks: Set<Network>,
    ): Map<Network, List<CryptoCurrency>> {
        val userWallet = userWalletsListRepository.getSyncStrict(id = userWalletId)

        // multi-currency wallet
        if (userWallet !is UserWallet.Cold || userWallet.isMultiCurrency) {
            return getMultiWalletCurrencies(userWallet = userWallet, networks = networks)
        }

        // check if the blockchain of single-currency wallet is the same as network
        val cardNetworkId = userWallet.scanResponse.cardTypesResolver.getBlockchain().toNetworkId()
        val cardNetwork = networks.firstOrNull { it.rawId == cardNetworkId }

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
        val userWallet = userWalletsListRepository.getSyncStrict(id = userWalletId)

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
        return listOf(createPrimaryCurrencyForSingleCurrencyCard(userWallet))
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

        val blockchains = getDefaultWalletBlockchains(userWallet, demoConfig)

        return blockchains.mapNotNull { blockchain ->
            cryptoCurrencyFactory.createCoin(
                blockchain = blockchain,
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
        val response = walletAccountsFetcher.getSaved(userWallet.walletId) ?: return emptyMap()

        val currenciesByNetworkId = createMultiWalletCurrencies(
            response = response,
            userWallet = userWallet,
            networkIds = networks.mapTo(hashSetOf(), Network::rawId),
        )
            .groupBy { it.network.id }

        return networks.associateWith { network ->
            currenciesByNetworkId[network.id].orEmpty()
        }
    }

    private suspend fun getMultiWalletCurrenciesByRawId(
        userWallet: UserWallet,
        rawIds: Set<Network.RawID>,
    ): Map<Network.RawID, List<CryptoCurrency>> {
        val response = walletAccountsFetcher.getSaved(userWallet.walletId) ?: return emptyMap()

        return createMultiWalletCurrencies(
            response = response,
            userWallet = userWallet,
            networkIds = rawIds.mapTo(hashSetOf()) { it.toBlockchain().toNetworkId() },
        )
            .groupBy { it.network.id.rawId }
    }

    private fun createMultiWalletCurrencies(
        response: GetWalletAccountsResponse,
        userWallet: UserWallet,
        networkIds: Set<String>,
    ): Set<CryptoCurrency> {
        return response.accounts.flatMapTo(hashSetOf()) { accountDTO ->
            val tokens = accountDTO.tokens.orEmpty().filter { token -> token.networkId in networkIds }

            when (accountDTO.type) {
                WalletAccountDTO.Type.JOINT -> createJointAccountCurrencies(accountDTO, userWallet, tokens)
                WalletAccountDTO.Type.CRYPTO, null -> createPersonalAccountCurrencies(accountDTO, userWallet, tokens)
            }
        }
    }

    private fun createPersonalAccountCurrencies(
        accountDTO: WalletAccountDTO,
        userWallet: UserWallet,
        tokens: List<UserTokensResponse.Token>,
    ): List<CryptoCurrency> {
        val accountIndex = DerivationIndex(accountDTO.derivationIndex).getOrNull() ?: return emptyList()

        return responseCryptoCurrenciesFactory.createCurrencies(
            tokens = tokens,
            userWallet = userWallet,
            accountIndex = accountIndex,
        )
    }

    private fun createJointAccountCurrencies(
        accountDTO: WalletAccountDTO,
        userWallet: UserWallet,
        tokens: List<UserTokensResponse.Token>,
    ): List<CryptoCurrency> {
        val path = OwnerKeyIndex(accountDTO.derivationIndex).getOrNull()?.toNetworkDerivationPath()
            ?: return emptyList()

        return responseCryptoCurrenciesFactory.createCurrencies(
            tokens = tokens,
            userWallet = userWallet,
            derivationPath = path,
        )
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