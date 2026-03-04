package com.tangem.data.tokens.repository

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionStatus
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.common.currency.CardCryptoCurrencyFactory
import com.tangem.data.common.currency.CryptoCurrencyFactory
import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.data.common.currency.getTokenId
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.local.token.UserTokensResponseStore
import com.tangem.domain.card.CardTypesResolver
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.core.error.DataError
import com.tangem.domain.express.ExpressServiceFetcher
import com.tangem.domain.express.models.ExpressAsset
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.models.wallet.requireColdWallet
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesProducer
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier
import com.tangem.domain.tokens.model.FeePaidCurrency
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import timber.log.Timber
import com.tangem.blockchain.common.FeePaidCurrency as FeePaidSdkCurrency

@Suppress("LargeClass", "LongParameterList", "TooManyFunctions")
internal class DefaultCurrenciesRepository(
    private val tangemTechApi: TangemTechApi,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val walletManagersFacade: WalletManagersFacade,
    private val cacheRegistry: CacheRegistry,
    private val expressServiceFetcher: ExpressServiceFetcher,
    private val dispatchers: CoroutineDispatcherProvider,
    private val cardCryptoCurrencyFactory: CardCryptoCurrencyFactory,
    private val userTokensResponseStore: UserTokensResponseStore,
    private val responseCryptoCurrenciesFactory: ResponseCryptoCurrenciesFactory,
    private val multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
    excludedBlockchains: ExcludedBlockchains,
) : CurrenciesRepository {

    private val cryptoCurrencyFactory = CryptoCurrencyFactory(excludedBlockchains)

    override fun getWalletCurrenciesUpdates(userWalletId: UserWalletId): Flow<List<CryptoCurrency>> {
        return channelFlow {
            val userWallet = userWalletsListRepository.getSyncStrict(userWalletId)

            if (userWallet.isMultiCurrency) {
                getMultiCurrencyWalletCurrenciesUpdates(userWalletId).collect(::send)
            } else {
                val currencies = getSingleCurrencyWalletWithCardCurrencies(userWalletId)

                send(currencies)
            }
        }
    }

    override suspend fun getSingleCurrencyWalletPrimaryCurrency(
        userWalletId: UserWalletId,
        refresh: Boolean,
    ): CryptoCurrency {
        return withContext(dispatchers.io) {
            val userWallet = userWalletsListRepository.getSyncStrict(userWalletId)
            userWallet.requireColdWallet()
            ensureIsCorrectUserWallet(userWallet, isMultiCurrencyWalletExpected = false)

            val currency = cardCryptoCurrencyFactory.createPrimaryCurrencyForSingleCurrencyCard(
                userWallet = userWallet,
            )

            fetchExpressAssetsByNetworkIds(
                userWallet = userWallet,
                cryptoCurrencies = listOf(currency),
                refresh = refresh,
            )

            currency
        }
    }

    override suspend fun getSingleCurrencyWalletWithCardCurrencies(
        userWalletId: UserWalletId,
        refresh: Boolean,
    ): List<CryptoCurrency> {
        return withContext(dispatchers.io) {
            val userWallet = userWalletsListRepository.getSyncStrict(userWalletId)
            val scanResponse = userWallet.requireColdWallet().scanResponse

            val currencies = if (scanResponse.cardTypesResolver.isSingleWalletWithToken()) {
                cardCryptoCurrencyFactory.createCurrenciesForSingleCurrencyCardWithToken(userWallet = userWallet)
            } else {
                cardCryptoCurrencyFactory.createPrimaryCurrencyForSingleCurrencyCard(userWallet = userWallet)
                    .run {
                        listOf(this)
                    }
            }

            fetchExpressAssetsByNetworkIds(
                userWallet = userWallet,
                cryptoCurrencies = currencies,
                refresh = refresh,
            )

            currencies
        }
    }

    override suspend fun getSingleCurrencyWalletWithCardCurrency(
        userWalletId: UserWalletId,
        id: CryptoCurrency.ID,
    ): CryptoCurrency {
        return withContext(dispatchers.io) {
            val userWallet = userWalletsListRepository.getSyncStrict(userWalletId)
            userWallet.requireColdWallet()
            ensureIsCorrectUserWallet(userWallet, isMultiCurrencyWalletExpected = false)

            val currency = cardCryptoCurrencyFactory.createCurrenciesForSingleCurrencyCardWithToken(
                userWallet = userWallet,
            )
                .find { it.id == id }
            requireNotNull(currency) { "Unable to find currency with provided ID: $id" }
            fetchExpressAssetsByNetworkIds(userWallet, listOf(currency))
            currency
        }
    }

    private fun getMultiCurrencyWalletCurrenciesUpdates(userWalletId: UserWalletId): Flow<List<CryptoCurrency>> {
        return channelFlow {
            val userWallet = userWalletsListRepository.getSyncStrict(userWalletId)
            ensureIsCorrectUserWallet(userWallet, isMultiCurrencyWalletExpected = true)

            getMultiCurrencyWalletCurrencies(userWallet)
                .onEach { send(it) }
                .launchIn(scope = this + dispatchers.io)
        }
    }

    override suspend fun getNetworkCoin(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
    ): CryptoCurrency.Coin {
        return multiWalletCryptoCurrenciesSupplier.getSyncOrNull(
            params = MultiWalletCryptoCurrenciesProducer.Params(userWalletId = userWalletId),
        )
            .orEmpty()
            .find { currency ->
                currency is CryptoCurrency.Coin &&
                    currency.network.id.rawId == networkId.rawId &&
                    currency.network.derivationPath == derivationPath
            } as? CryptoCurrency.Coin
            ?: error("Unable to find coin for network ID: $networkId")
    }

    override suspend fun isSendBlockedByPendingTransactions(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): Boolean {
        val blockchain = cryptoCurrencyStatus.currency.network.toBlockchain()
        val isBitcoinBlockchain =
            blockchain == Blockchain.Bitcoin || blockchain == Blockchain.BitcoinTestnet
        return when {
            cryptoCurrencyStatus.currency is CryptoCurrency.Coin && isBitcoinBlockchain -> {
                val outgoingTransactions =
                    cryptoCurrencyStatus.value.pendingTransactions.filter { it.isOutgoing }
                outgoingTransactions.isNotEmpty()
            }

            blockchain.isEvm() -> false
            blockchain == Blockchain.Tron || blockchain == Blockchain.TronTestnet -> false
            else -> {
                val walletManager = walletManagersFacade.getOrCreateWalletManager(
                    userWalletId = userWalletId,
                    network = cryptoCurrencyStatus.currency.network,
                ) ?: return false

                walletManager.wallet.recentTransactions.any { it.status == TransactionStatus.Unconfirmed }
            }
        }
    }

    override suspend fun getFeePaidCurrency(userWalletId: UserWalletId, network: Network): FeePaidCurrency {
        return withContext(dispatchers.io) {
            val blockchain = network.toBlockchain()
            when (val feePaidCurrency = blockchain.feePaidCurrency()) {
                FeePaidSdkCurrency.Coin -> FeePaidCurrency.Coin
                FeePaidSdkCurrency.SameCurrency -> FeePaidCurrency.SameCurrency
                is FeePaidSdkCurrency.Token -> {
                    val balance = walletManagersFacade.tokenBalance(
                        userWalletId = userWalletId,
                        network = network,
                        name = feePaidCurrency.token.name,
                        symbol = feePaidCurrency.token.symbol,
                        contractAddress = feePaidCurrency.token.contractAddress,
                        decimals = feePaidCurrency.token.decimals,
                        id = feePaidCurrency.token.id,
                    )
                    FeePaidCurrency.Token(
                        tokenId = getTokenId(network = network, sdkToken = feePaidCurrency.token),
                        name = feePaidCurrency.token.name,
                        symbol = feePaidCurrency.token.symbol,
                        contractAddress = feePaidCurrency.token.contractAddress,
                        balance = balance,
                    )
                }

                is FeePaidSdkCurrency.FeeResource -> FeePaidCurrency.FeeResource(currency = feePaidCurrency.currency)
            }
        }
    }

    override fun createCoinCurrency(network: Network): CryptoCurrency.Coin {
        return cryptoCurrencyFactory.createCoin(network = network)
    }

    override fun createTokenCurrency(cryptoCurrency: CryptoCurrency.Token, network: Network): CryptoCurrency.Token {
        return cryptoCurrencyFactory.createToken(
            cryptoCurrency = cryptoCurrency,
            network = network,
        )
    }

    override suspend fun createTokenCurrency(
        userWalletId: UserWalletId,
        contractAddress: String,
        networkId: String,
    ): CryptoCurrency.Token {
        val userWallet = userWalletsListRepository.getSyncStrict(userWalletId)
        val token = withContext(dispatchers.io) {
            val foundToken = tangemTechApi.getCoins(
                contractAddress = contractAddress,
                networkId = networkId,
            )
                .getOrThrow()
                .coins
                .firstOrNull()
                ?: error("Token not found")
            val network = foundToken.networks.firstOrNull { it.networkId == networkId }
                ?: error("Network not found")
            CryptoCurrencyFactory.Token(
                symbol = foundToken.symbol,
                name = foundToken.name,
                contractAddress = contractAddress,
                decimals = network.decimalCount?.toInt() ?: error("Decimals not found"),
                id = foundToken.id,
            )
        }
        return cryptoCurrencyFactory.createToken(
            token = token,
            networkId = networkId,
            extraDerivationPath = null,
            userWallet = userWallet,
        ) ?: error("Unable to create token")
    }

    override fun isNetworkFeeZero(userWalletId: UserWalletId, network: Network): Boolean {
        val blockchain = Blockchain.fromNetworkId(network.backendId)
        return blockchain?.isNetworkFeeZero() == true
    }

    override fun getCardTypesResolver(userWalletId: UserWalletId): CardTypesResolver? {
        return (userWalletsListRepository.getSyncStrict(userWalletId) as? UserWallet.Cold)?.cardTypesResolver
    }

    private fun getMultiCurrencyWalletCurrencies(userWallet: UserWallet): Flow<List<CryptoCurrency>> {
        return getSavedUserTokensResponse(userWallet.walletId).map { storedTokens ->
            responseCryptoCurrenciesFactory.createCurrencies(
                response = storedTokens,
                userWallet = userWallet,
                accountIndex = DerivationIndex.Main,
            )
        }
    }

    private suspend fun fetchExpressAssetsByNetworkIds(
        userWallet: UserWallet,
        cryptoCurrencies: List<CryptoCurrency>,
        refresh: Boolean = false,
    ) {
        val tokens = cryptoCurrencies.mapTo(hashSetOf()) { currency ->
            val tokenCurrency = currency as? CryptoCurrency.Token
            ExpressAsset.ID(
                networkId = currency.network.backendId,
                contractAddress = tokenCurrency?.contractAddress,
            )
        }
        cacheRegistry.invokeOnExpire(
            key = getAssetsCacheKey(userWallet.walletId),
            skipCache = refresh,
            block = {
                coroutineScope {
                    launch { expressServiceFetcher.fetch(userWallet, tokens) }
                }
            },
        )
    }

    private fun getAssetsCacheKey(userWalletId: UserWalletId): String = "assets_cache_key_${userWalletId.stringValue}"

    private fun ensureIsCorrectUserWallet(userWallet: UserWallet, isMultiCurrencyWalletExpected: Boolean) {
        val userWalletId = userWallet.walletId

        val message = when {
            !userWallet.isMultiCurrency && isMultiCurrencyWalletExpected -> {
                "Multi currency wallet expected, but single currency wallet was found: $userWalletId"
            }

            userWallet.isMultiCurrency && !isMultiCurrencyWalletExpected -> {
                "Single currency wallet expected, but multi currency wallet was found: $userWalletId"
            }

            else -> null
        }

        if (message != null) {
            val error = DataError.UserWalletError.WrongUserWallet(message)

            Timber.e(error)
            throw error
        }
    }

    private fun getSavedUserTokensResponse(key: UserWalletId): Flow<UserTokensResponse> {
        return userTokensResponseStore.get(userWalletId = key).filterNotNull()
    }
}