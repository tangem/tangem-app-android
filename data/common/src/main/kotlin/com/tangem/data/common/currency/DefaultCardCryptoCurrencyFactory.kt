package com.tangem.data.common.currency

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.datasource.local.token.UserTokensResponseStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.common.TapWorkarounds.isTestCard
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.demo.DemoConfig
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId

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
) : CardCryptoCurrencyFactory {

    private val cryptoCurrencyFactory by lazy { CryptoCurrencyFactory(excludedBlockchains) }

    override suspend fun create(userWalletId: UserWalletId, network: Network): List<CryptoCurrency> {
        val userWallet = userWalletsStore.getSyncStrict(key = userWalletId)

        val blockchain = Blockchain.fromNetworkId(networkId = network.backendId)

        // multi-currency wallet
        if (userWallet.isMultiCurrency) return getMultiWalletCurrencies(userWallet = userWallet, network = network)

        // check if the blockchain of single-currency wallet is the same as network
        val cardBlockchain = userWallet.scanResponse.cardTypesResolver.getBlockchain()
        if (cardBlockchain != blockchain) return emptyList()

        // single-currency wallet with token (NODL)
        if (userWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken()) {
            return createCurrenciesForSingleCurrencyCardWithToken(userWallet.scanResponse)
        }

        // single-currency wallet
        return createPrimaryCurrencyForSingleCurrencyCard(userWallet.scanResponse).let(::listOf)
    }

    override fun createDefaultCoinsForMultiCurrencyCard(scanResponse: ScanResponse): List<CryptoCurrency.Coin> {
        val card = scanResponse.card

        var blockchains = if (demoConfig.isDemoCardId(card.cardId)) {
            demoConfig.demoBlockchains
        } else {
            listOf(Blockchain.Bitcoin, Blockchain.Ethereum)
        }

        if (card.isTestCard) {
            blockchains = blockchains.mapNotNull { it.getTestnetVersion() }
        }

        return blockchains.mapNotNull {
            cryptoCurrencyFactory.createCoin(
                blockchain = it,
                extraDerivationPath = null,
                scanResponse = scanResponse,
            )
        }
    }

    override fun createPrimaryCurrencyForSingleCurrencyCard(scanResponse: ScanResponse): CryptoCurrency {
        return with(getSingleWalletCurrencies(scanResponse)) {
            primaryToken ?: coin
        }
    }

    override fun createCurrenciesForSingleCurrencyCardWithToken(scanResponse: ScanResponse): List<CryptoCurrency> {
        return with(getSingleWalletCurrencies(scanResponse)) {
            listOfNotNull(coin, primaryToken)
        }
    }

    private suspend fun getMultiWalletCurrencies(userWallet: UserWallet, network: Network): List<CryptoCurrency> {
        val response = userTokensResponseStore.getSyncOrNull(userWalletId = userWallet.walletId)
            ?: return emptyList()

        val responseCurrenciesFactory = ResponseCryptoCurrenciesFactory(excludedBlockchains)

        return responseCurrenciesFactory.createCurrencies(
            tokens = response.tokens.filter {
                it.networkId == network.backendId && it.derivationPath == network.derivationPath.value
            },
            scanResponse = userWallet.scanResponse,
        )
    }

    private fun getSingleWalletCurrencies(scanResponse: ScanResponse): SingleWalletCurrencies {
        val resolver = scanResponse.cardTypesResolver
        val blockchain = resolver.getBlockchain()

        val coin = cryptoCurrencyFactory.createCoin(
            blockchain = blockchain,
            extraDerivationPath = null,
            scanResponse = scanResponse,
        )

        requireNotNull(coin) { "Coin for the single currency card cannot be null" }

        val primaryToken = resolver.getPrimaryToken()?.let { token ->
            cryptoCurrencyFactory.createToken(
                sdkToken = token,
                blockchain = blockchain,
                extraDerivationPath = null,
                scanResponse = scanResponse,
            )
        }

        return SingleWalletCurrencies(coin = coin, primaryToken = primaryToken)
    }

    private data class SingleWalletCurrencies(val coin: CryptoCurrency, val primaryToken: CryptoCurrency?)
}