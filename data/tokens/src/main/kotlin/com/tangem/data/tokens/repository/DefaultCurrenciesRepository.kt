package com.tangem.data.tokens.repository

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionStatus
import com.tangem.blockchainsdk.compatibility.getL2CompatibilityTokenComparison
import com.tangem.blockchainsdk.utils.*
import com.tangem.data.common.api.safeApiCall
import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.common.currency.*
import com.tangem.data.tokens.utils.CustomTokensMerger
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.express.models.TangemExpressValues.EMPTY_CONTRACT_ADDRESS_VALUE
import com.tangem.datasource.api.express.models.request.LeastTokenInfo
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.exchangeservice.swap.ExpressServiceLoader
import com.tangem.datasource.local.token.UserTokensResponseStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.card.CardTypesResolver
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.core.error.DataError
import com.tangem.domain.demo.models.DemoConfig
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.*
import com.tangem.domain.tokens.model.FeePaidCurrency
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import com.tangem.blockchain.common.FeePaidCurrency as FeePaidSdkCurrency

@Suppress("LargeClass", "LongParameterList", "TooManyFunctions")
internal class DefaultCurrenciesRepository(
    private val tangemTechApi: TangemTechApi,
    private val userWalletsStore: UserWalletsStore,
    private val walletManagersFacade: WalletManagersFacade,
    private val cacheRegistry: CacheRegistry,
    private val expressServiceLoader: ExpressServiceLoader,
    private val dispatchers: CoroutineDispatcherProvider,
    private val cardCryptoCurrencyFactory: CardCryptoCurrencyFactory,
    private val userTokensSaver: UserTokensSaver,
    private val userTokensResponseStore: UserTokensResponseStore,
    private val responseCryptoCurrenciesFactory: ResponseCryptoCurrenciesFactory,
    excludedBlockchains: ExcludedBlockchains,
) : CurrenciesRepository {

    private val demoConfig = DemoConfig()
    private val cryptoCurrencyFactory = CryptoCurrencyFactory(excludedBlockchains)
    private val userTokensResponseFactory = UserTokensResponseFactory()
    private val customTokensMerger = CustomTokensMerger(
        tangemTechApi = tangemTechApi,
        dispatchers = dispatchers,
        userTokensSaver = userTokensSaver,
    )

    override suspend fun saveTokens(
        userWalletId: UserWalletId,
        currencies: List<CryptoCurrency>,
        isGroupedByNetwork: Boolean,
        isSortedByBalance: Boolean,
    ) = withContext(dispatchers.io) {
        ensureIsCorrectUserWallet(userWalletId, isMultiCurrencyWalletExpected = true)

        val response = userTokensResponseFactory.createUserTokensResponse(
            currencies = currencies,
            isGroupedByNetwork = isGroupedByNetwork,
            isSortedByBalance = isSortedByBalance,
        )
        userTokensSaver.storeAndPush(userWalletId, response)
    }

    override suspend fun saveNewCurrenciesList(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        withContext(dispatchers.io) {
            val savedResponse = requireNotNull(
                value = getSavedUserTokensResponseSync(key = userWalletId),
                lazyMessage = { "Saved tokens empty. Can not perform add currencies action." },
            )

            val newCurrencies = populateCurrenciesWithMissedCoins(currencies)

            val updatedResponse = savedResponse.copy(
                tokens = newCurrencies.map(userTokensResponseFactory::createResponseToken),
            )
            userTokensSaver.storeAndPush(
                userWalletId = userWalletId,
                response = updatedResponse,
            )

            fetchExpressAssetsByNetworkIds(
                userWallet = userWalletsStore.getSyncStrict(key = userWalletId),
                userTokens = updatedResponse,
            )
        }
    }

    override suspend fun addCurrencies(
        userWalletId: UserWalletId,
        currencies: List<CryptoCurrency>,
    ): List<CryptoCurrency> = withContext(dispatchers.io) {
        val savedCurrencies = requireNotNull(
            value = getSavedUserTokensResponseSync(key = userWalletId),
            lazyMessage = { "Saved tokens empty. Can not perform add currencies action" },
        )

        val currenciesToAdd = filterAlreadyAddedCurrencies(
            savedCurrencies = savedCurrencies.tokens,
            currenciesToAdd = populateCurrenciesWithMissedCoins(currencies = currencies),
        )

        val updatedResponse = savedCurrencies.copy(
            tokens = savedCurrencies.tokens + currenciesToAdd.map(userTokensResponseFactory::createResponseToken),
        )

        userTokensSaver.storeAndPush(
            userWalletId = userWalletId,
            response = updatedResponse,
        )

        fetchExpressAssetsByNetworkIds(
            userWallet = userWalletsStore.getSyncStrict(key = userWalletId),
            userTokens = updatedResponse,
        )

        currenciesToAdd
    }

    override suspend fun saveNewCurrenciesListCache(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        withContext(dispatchers.io) {
            val savedResponse = requireNotNull(
                value = getSavedUserTokensResponseSync(key = userWalletId),
                lazyMessage = { "Saved tokens empty. Can not perform add currencies action." },
            )

            val newCurrencies = populateCurrenciesWithMissedCoins(currencies)

            val updatedResponse = savedResponse.copy(
                tokens = newCurrencies.map(userTokensResponseFactory::createResponseToken),
            )
            userTokensSaver.store(
                userWalletId = userWalletId,
                response = updatedResponse,
            )

            fetchExpressAssetsByNetworkIds(
                userWallet = userWalletsStore.getSyncStrict(key = userWalletId),
                userTokens = updatedResponse,
            )
        }
    }

    override suspend fun addCurrenciesCache(
        userWalletId: UserWalletId,
        currencies: List<CryptoCurrency>,
    ): List<CryptoCurrency> = withContext(dispatchers.io) {
        val savedCurrencies = requireNotNull(
            value = getSavedUserTokensResponseSync(key = userWalletId),
            lazyMessage = { "Saved tokens empty. Can not perform add currencies action" },
        )

        val currenciesToAdd = filterAlreadyAddedCurrencies(
            savedCurrencies = savedCurrencies.tokens,
            currenciesToAdd = populateCurrenciesWithMissedCoins(currencies = currencies),
        )

        val updatedResponse = savedCurrencies.copy(
            tokens = savedCurrencies.tokens + currenciesToAdd.map(userTokensResponseFactory::createResponseToken),
        )

        userTokensSaver.store(
            userWalletId = userWalletId,
            response = updatedResponse,
        )

        fetchExpressAssetsByNetworkIds(
            userWallet = userWalletsStore.getSyncStrict(key = userWalletId),
            userTokens = updatedResponse,
        )

        currenciesToAdd
    }

    private fun filterAlreadyAddedCurrencies(
        savedCurrencies: List<UserTokensResponse.Token>,
        currenciesToAdd: List<CryptoCurrency>,
    ): List<CryptoCurrency> {
        return currenciesToAdd.filter { currency ->
            val networkId = currency.network.toBlockchain().toNetworkId()
            val contractAddress = (currency as? CryptoCurrency.Token)?.contractAddress

            savedCurrencies.none { token ->
                token.contractAddress == contractAddress &&
                    token.networkId == networkId &&
                    token.derivationPath == currency.network.derivationPath.value
            }
        }
    }

    private fun populateCurrenciesWithMissedCoins(currencies: List<CryptoCurrency>): List<CryptoCurrency> {
        val currenciesSequence = currencies.asSequence()

        val networksWithTokens = currenciesSequence
            .filterIsInstance<CryptoCurrency.Token>()
            .map { it.network }
            .distinct()

        val networksWithCoins = currenciesSequence
            .filterIsInstance<CryptoCurrency.Coin>()
            .map { it.network }
            .distinct()

        val networksNeedingCoins = (networksWithTokens - networksWithCoins.toSet()).toMutableList()

        if (networksNeedingCoins.isEmpty()) return currencies

        return buildList {
            currencies.forEach { currency ->
                if (currency is CryptoCurrency.Token && currency.network in networksNeedingCoins) {
                    val coin = cryptoCurrencyFactory.createCoin(currency.network)
                    add(coin)

                    networksNeedingCoins.remove(currency.network)
                }

                add(currency)
            }
        }
    }

    override suspend fun removeCurrency(userWalletId: UserWalletId, currency: CryptoCurrency) =
        withContext(dispatchers.io) {
            val savedCurrencies = requireNotNull(
                value = getSavedUserTokensResponseSync(key = userWalletId),
                lazyMessage = { "Saved tokens empty. Can not perform remove currency action" },
            )

            val token = userTokensResponseFactory.createResponseToken(currency)
            val updatedResponse =
                savedCurrencies.copy(tokens = savedCurrencies.tokens.filterNot { it == token })
            userTokensSaver.storeAndPush(
                userWalletId = userWalletId,
                response = updatedResponse,
            )
        }

    override suspend fun removeCurrencies(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        return withContext(dispatchers.io) {
            val savedCurrencies = requireNotNull(
                value = getSavedUserTokensResponseSync(key = userWalletId),
                lazyMessage = { "Saved tokens empty. Can not perform remove currencies action" },
            )

            val tokens = currencies.map(userTokensResponseFactory::createResponseToken)
            val updatedResponse = savedCurrencies.copy(
                tokens = savedCurrencies.tokens.filterNot(tokens::contains),
            )
            userTokensSaver.storeAndPush(
                userWalletId = userWalletId,
                response = updatedResponse,
            )
        }
    }

    override fun getWalletCurrenciesUpdates(userWalletId: UserWalletId): Flow<List<CryptoCurrency>> {
        return channelFlow {
            val userWallet = userWalletsStore.getSyncStrict(userWalletId)

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
            val userWallet = userWalletsStore.getSyncStrict(userWalletId)
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
            val userWallet = userWalletsStore.getSyncStrict(userWalletId)
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
            val userWallet = userWalletsStore.getSyncStrict(userWalletId)
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

    override fun getMultiCurrencyWalletCurrenciesUpdates(userWalletId: UserWalletId): Flow<List<CryptoCurrency>> {
        return channelFlow {
            val userWallet = userWalletsStore.getSyncStrict(userWalletId)
            ensureIsCorrectUserWallet(userWallet, isMultiCurrencyWalletExpected = true)

            getMultiCurrencyWalletCurrencies(userWallet)
                .onEach { send(it) }
                .launchIn(scope = this + dispatchers.io)

            withContext(dispatchers.io) {
                fetchTokensIfCacheExpired(userWallet, refresh = false)
            }
        }
    }

    override suspend fun getMultiCurrencyWalletCurrenciesSync(
        userWalletId: UserWalletId,
        refresh: Boolean,
    ): List<CryptoCurrency> = withContext(dispatchers.io) {
        val userWallet = userWalletsStore.getSyncStrict(userWalletId)
        ensureIsCorrectUserWallet(userWallet, isMultiCurrencyWalletExpected = true)

        fetchTokensIfCacheExpired(userWallet, refresh)

        val storedTokens = requireNotNull(
            value = getSavedUserTokensResponseSync(key = userWallet.walletId),
            lazyMessage = {
                "Unable to find tokens response for user wallet with provided ID: $userWalletId"
            },
        )

        responseCryptoCurrenciesFactory.createCurrencies(
            storedTokens,
            userWallet = userWallet,
        )
    }

    override suspend fun getMultiCurrencyWalletCachedCurrenciesSync(userWalletId: UserWalletId) =
        withContext(dispatchers.io) {
            val userWallet = userWalletsStore.getSyncStrict(userWalletId)
            ensureIsCorrectUserWallet(userWallet, isMultiCurrencyWalletExpected = true)

            val storedTokens = requireNotNull(
                value = getSavedUserTokensResponseSync(key = userWallet.walletId),
                lazyMessage = {
                    "Unable to find tokens response for user wallet with provided ID: $userWalletId"
                },
            )

            responseCryptoCurrenciesFactory.createCurrencies(
                storedTokens,
                userWallet = userWallet,
            )
        }

    override suspend fun getMultiCurrencyWalletCurrency(
        userWalletId: UserWalletId,
        id: CryptoCurrency.ID,
    ): CryptoCurrency = withContext(dispatchers.io) {
        getMultiCurrencyWalletCurrency(userWalletId, id.value)
    }

    override suspend fun getMultiCurrencyWalletCurrency(userWalletId: UserWalletId, id: String): CryptoCurrency =
        withContext(dispatchers.io) {
            val userWallet = userWalletsStore.getSyncStrict(userWalletId)
            ensureIsCorrectUserWallet(userWallet, isMultiCurrencyWalletExpected = true)

            val response = requireNotNull(
                value = getSavedUserTokensResponseSync(key = userWalletId),
                lazyMessage = {
                    "Unable to find tokens response for user wallet with provided ID: $userWalletId"
                },
            )

            responseCryptoCurrenciesFactory.createCurrency(
                currencyId = id,
                response = response,
                userWallet = userWallet,
            )
        }

    override suspend fun getNetworkCoin(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
    ): CryptoCurrency.Coin {
        return withContext(dispatchers.io) {
            val userWallet = userWalletsStore.getSyncStrict(userWalletId)
            ensureIsCorrectUserWallet(userWallet = userWallet, isMultiCurrencyWalletExpected = true)

            fetchTokensIfCacheExpired(userWallet = userWallet, refresh = false)

            val storedTokens = requireNotNull(
                value = getSavedUserTokensResponseSync(key = userWalletId),
                lazyMessage = {
                    "Unable to find tokens response for user wallet with provided ID: $userWalletId"
                },
            )
            val blockchain = networkId.toBlockchain()
            val blockchainNetworkId = blockchain.toNetworkId()
            val coinId = blockchain.toCoinId()

            val storedCoin = storedTokens.tokens
                .find {
                    it.networkId == blockchainNetworkId &&
                        compareIdWithMigrations(it, coinId) &&
                        it.derivationPath == derivationPath.value
                } ?: error("Coin in this network $networkId not found")

            val coin = responseCryptoCurrenciesFactory.createCurrency(
                responseToken = storedCoin,
                userWallet = userWallet,
            )

            coin as? CryptoCurrency.Coin ?: error("Unable to create currency")
        }
    }

    override fun isTokensGrouped(userWalletId: UserWalletId): Flow<Boolean> {
        return channelFlow {
            val userWallet = userWalletsStore.getSyncStrict(userWalletId)

            if (userWallet.isMultiCurrency) {
                getSavedUserTokensResponse(userWalletId)
                    .map { response -> response.group == UserTokensResponse.GroupType.NETWORK }
                    .distinctUntilChanged()
                    .onEach { isGrouped -> send(isGrouped) }
                    .launchIn(scope = this + dispatchers.io)
            } else {
                send(element = false)
            }
        }
    }

    override fun isTokensSortedByBalance(userWalletId: UserWalletId): Flow<Boolean> {
        return channelFlow {
            val userWallet = userWalletsStore.getSyncStrict(userWalletId)

            if (userWallet.isMultiCurrency) {
                getSavedUserTokensResponse(userWalletId)
                    .map { response -> response.sort == UserTokensResponse.SortType.BALANCE }
                    .distinctUntilChanged()
                    .onEach { isSorted -> send(isSorted) }
                    .launchIn(scope = this + dispatchers.io)
            } else {
                send(element = false)
            }
        }
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
        val userWallet = userWalletsStore.getSyncStrict(userWalletId)
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

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getAllWalletsCryptoCurrencies(
        currencyRawId: CryptoCurrency.RawID,
    ): Flow<Map<UserWallet, List<CryptoCurrency>>> {
        return userWalletsStore.userWallets.flatMapLatest { userWallets ->

            userWallets.filter { it.isMultiCurrency }
                .forEach { fetchTokensIfCacheExpired(userWallet = it, refresh = false) }

            val userWalletsWithCurrencies = userWallets
                .filterNot(UserWallet::isLocked)
                .map { userWallet ->
                    getCurrenciesForWallet(userWallet, currencyRawId).map { userWallet to it }
                }

            combine(userWalletsWithCurrencies) { it.toMap() }
                .onEmpty { emit(value = emptyMap()) }
        }
    }

    private suspend fun getCurrenciesForWallet(
        userWallet: UserWallet,
        currencyRawId: CryptoCurrency.RawID,
    ): Flow<List<CryptoCurrency>> {
        return when {
            userWallet.isMultiCurrency -> {
                getSavedUserTokensResponse(userWallet.walletId).map { storedTokens ->
                    val filterResponse = storedTokens.tokens.filter {
                        getL2CompatibilityTokenComparison(it, currencyRawId.value)
                    }

                    responseCryptoCurrenciesFactory.createCurrencies(
                        response = storedTokens.copy(tokens = filterResponse),
                        userWallet = userWallet,
                    )
                }
            }

            else -> {
                val currencies =
                    if (userWallet.requireColdWallet().scanResponse.cardTypesResolver.isSingleWalletWithToken()) {
                        getSingleCurrencyWalletWithCardCurrencies(userWallet.walletId)
                    } else {
                        val currency =
                            getSingleCurrencyWalletPrimaryCurrency(userWalletId = userWallet.walletId)

                        if (currency.id.rawCurrencyId == currencyRawId) {
                            listOf(currency)
                        } else {
                            emptyList()
                        }
                    }
                flow {
                    emit(currencies)
                }
            }
        }
    }

    override fun isNetworkFeeZero(userWalletId: UserWalletId, network: Network): Boolean {
        val blockchain = Blockchain.fromNetworkId(network.backendId)
        return blockchain?.isNetworkFeeZero() == true
    }

    override suspend fun syncTokens(userWalletId: UserWalletId) {
        runCatching {
            val savedCurrencies = requireNotNull(
                value = getSavedUserTokensResponseSync(key = userWalletId),
                lazyMessage = { "Saved tokens empty. Can not perform add currencies action" },
            )
            userTokensSaver.storeAndPush(
                userWalletId = userWalletId,
                response = savedCurrencies,
            )
        }
    }

    override fun getCardTypesResolver(userWalletId: UserWalletId): CardTypesResolver? {
        return (userWalletsStore.getSyncStrict(userWalletId) as? UserWallet.Cold)?.cardTypesResolver
    }

    private fun getMultiCurrencyWalletCurrencies(userWallet: UserWallet): Flow<List<CryptoCurrency>> {
        return getSavedUserTokensResponse(userWallet.walletId).map { storedTokens ->
            responseCryptoCurrenciesFactory.createCurrencies(
                response = storedTokens,
                userWallet = userWallet,
            )
        }
    }

    private suspend fun fetchTokensIfCacheExpired(userWallet: UserWallet, refresh: Boolean) {
        cacheRegistry.invokeOnExpire(
            key = getTokensCacheKey(userWallet.walletId),
            skipCache = refresh,
            block = { fetchTokens(userWallet) },
        )
    }

    private fun compareIdWithMigrations(token: UserTokensResponse.Token, coinId: String): Boolean {
        return when {
            token.id == OLD_POLYGON_NAME -> NEW_POLYGON_NAME == coinId
            else -> token.id == coinId
        }
    }

    private suspend fun fetchTokens(userWallet: UserWallet) {
        val userWalletId = userWallet.walletId

        val response = if (userWallet is UserWallet.Cold && checkIsEmptyDemoWallet(userWallet)) {
            createDefaultUserTokensResponse(userWallet)
        } else {
            safeApiCall({ tangemTechApi.getUserTokens(userWalletId.stringValue).bind() }) {
                handleFetchTokensError(userWallet, it)
            }
        }

        val compatibleUserTokensResponse = response
            .let { it.copy(tokens = it.tokens.distinct()) }
            .let { customTokensMerger.mergeIfPresented(userWalletId, it) }

        userTokensSaver.store(userWalletId, compatibleUserTokensResponse)

        fetchExpressAssetsByNetworkIds(userWallet, compatibleUserTokensResponse)
    }

    private suspend fun checkIsEmptyDemoWallet(userWallet: UserWallet.Cold): Boolean {
        val response = getSavedUserTokensResponseSync(key = userWallet.walletId)

        return demoConfig.isDemoCardId(userWallet.cardId) && response == null
    }

    private suspend fun fetchExpressAssetsByNetworkIds(userWallet: UserWallet, userTokens: UserTokensResponse) {
        val tokens = userTokens.tokens.map { token ->
            LeastTokenInfo(
                contractAddress = token.contractAddress ?: EMPTY_CONTRACT_ADDRESS_VALUE,
                network = token.networkId,
            )
        }

        coroutineScope {
            launch { expressServiceLoader.update(userWallet, tokens) }
        }
    }

    private suspend fun fetchExpressAssetsByNetworkIds(
        userWallet: UserWallet,
        cryptoCurrencies: List<CryptoCurrency>,
        refresh: Boolean = false,
    ) {
        val tokens = cryptoCurrencies.map { currency ->
            val tokenCurrency = currency as? CryptoCurrency.Token
            LeastTokenInfo(
                contractAddress = tokenCurrency?.contractAddress ?: EMPTY_CONTRACT_ADDRESS_VALUE,
                network = currency.network.backendId,
            )
        }
        cacheRegistry.invokeOnExpire(
            key = getAssetsCacheKey(userWallet.walletId),
            skipCache = refresh,
            block = {
                coroutineScope {
                    launch { expressServiceLoader.update(userWallet, tokens) }
                }
            },
        )
    }

    private fun getAssetsCacheKey(userWalletId: UserWalletId): String = "assets_cache_key_${userWalletId.stringValue}"

    private suspend fun handleFetchTokensError(userWallet: UserWallet, e: ApiResponseError): UserTokensResponse {
        val userWalletId = userWallet.walletId
        val response = userTokensResponseStore.getSyncOrNull(userWalletId = userWalletId)
            ?: createDefaultUserTokensResponse(userWallet = userWallet)

        if (e is ApiResponseError.HttpException && e.code == ApiResponseError.HttpException.Code.NOT_FOUND) {
            Timber.w(
                e,
                "Requested currencies could not be found in the remote store for: $userWalletId",
            )

            userTokensSaver.push(userWalletId, response)
        } else {
            cacheRegistry.invalidate(getTokensCacheKey(userWalletId))
        }

        return response
    }

    private fun createDefaultUserTokensResponse(userWallet: UserWallet) =
        userTokensResponseFactory.createUserTokensResponse(
            currencies = cardCryptoCurrencyFactory.createDefaultCoinsForMultiCurrencyWallet(
                userWallet = userWallet,
            ),
            isGroupedByNetwork = false,
            isSortedByBalance = false,
        )

    private fun ensureIsCorrectUserWallet(userWalletId: UserWalletId, isMultiCurrencyWalletExpected: Boolean) {
        val userWallet = userWalletsStore.getSyncStrict(userWalletId)

        ensureIsCorrectUserWallet(userWallet, isMultiCurrencyWalletExpected)
    }

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

    private fun getTokensCacheKey(userWalletId: UserWalletId): String = "tokens_cache_key_${userWalletId.stringValue}"

    private fun getSavedUserTokensResponse(key: UserWalletId): Flow<UserTokensResponse> {
        return userTokensResponseStore.get(userWalletId = key).filterNotNull()
    }

    private suspend fun getSavedUserTokensResponseSync(key: UserWalletId): UserTokensResponse? {
        return userTokensResponseStore.getSyncOrNull(userWalletId = key)
    }
}