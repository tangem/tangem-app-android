package com.tangem.data.tokens.repository

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.compatibility.getL2CompatibilityTokenComparison
import com.tangem.blockchainsdk.utils.toCoinId
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.data.common.api.safeApiCall
import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.common.currency.*
import com.tangem.data.tokens.utils.CardCryptoCurrenciesFactory
import com.tangem.data.tokens.utils.CustomTokensMerger
import com.tangem.data.tokens.utils.UserTokensBackwardCompatibility
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.express.TangemExpressApi
import com.tangem.datasource.api.express.models.TangemExpressValues.EMPTY_CONTRACT_ADDRESS_VALUE
import com.tangem.datasource.api.express.models.request.AssetsRequestBody
import com.tangem.datasource.api.express.models.request.LeastTokenInfo
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObject
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrNull
import com.tangem.datasource.local.preferences.utils.storeObject
import com.tangem.datasource.local.token.ExpressAssetsStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.core.error.DataError
import com.tangem.domain.demo.DemoConfig
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.FeePaidCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import timber.log.Timber
import com.tangem.blockchain.common.FeePaidCurrency as FeePaidSdkCurrency

@Suppress("LargeClass", "LongParameterList", "TooManyFunctions")
internal class DefaultCurrenciesRepository(
    private val tangemTechApi: TangemTechApi,
    private val tangemExpressApi: TangemExpressApi,
    private val userWalletsStore: UserWalletsStore,
    private val walletManagersFacade: WalletManagersFacade,
    private val expressAssetsStore: ExpressAssetsStore,
    private val cacheRegistry: CacheRegistry,
    private val appPreferencesStore: AppPreferencesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : CurrenciesRepository {

    private val demoConfig = DemoConfig()
    private val responseCurrenciesFactory = ResponseCryptoCurrenciesFactory()
    private val cryptoCurrencyFactory = CryptoCurrencyFactory()
    private val cardCurrenciesFactory = CardCryptoCurrenciesFactory(demoConfig)
    private val userTokensResponseFactory = UserTokensResponseFactory()
    private val userTokensBackwardCompatibility = UserTokensBackwardCompatibility()
    private val customTokensMerger = CustomTokensMerger(tangemTechApi, dispatchers)

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

        storeAndPushTokens(userWalletId, response)
    }

    override suspend fun saveNewCurrenciesList(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        val savedResponse = requireNotNull(
            value = getSavedUserTokensResponseSync(key = userWalletId),
            lazyMessage = { "Saved tokens empty. Can not perform add currencies action" },
        )
        val newCoins = createCoinsForNewTokenList(
            userWalletId = userWalletId,
            newTokens = currencies.filterIsInstance<CryptoCurrency.Token>(),
        )
        val newCurrencies = (newCoins + currencies).distinct()
        val updatedResponse = savedResponse.copy(
            tokens = newCurrencies.map(userTokensResponseFactory::createResponseToken),
        )
        storeAndPushTokens(
            userWalletId = userWalletId,
            response = updatedResponse,
        )
        fetchExchangeableUserMarketCoinsByIds(userWalletId, updatedResponse)
    }

    override suspend fun addCurrencies(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        return withContext(dispatchers.io) {
            val savedCurrencies = requireNotNull(
                value = getSavedUserTokensResponseSync(key = userWalletId),
                lazyMessage = { "Saved tokens empty. Can not perform add currencies action" },
            )

            val filteredCurrencies = currencies.toMutableList()
            filteredCurrencies.filterNot { currency ->
                val blockchain = getBlockchain(networkId = currency.network.id)
                val networkId = blockchain.toNetworkId()
                val contractAddress = (currency as? CryptoCurrency.Token)?.contractAddress
                savedCurrencies.tokens.firstOrNull { token ->
                    token.contractAddress == contractAddress &&
                        token.networkId == networkId &&
                        token.derivationPath == currency.network.derivationPath.value
                } != null
            }

            val newCoins = createCoinsForNewTokens(
                userWalletId = userWalletId,
                newTokens = filteredCurrencies.filterIsInstance<CryptoCurrency.Token>(),
                savedCurrencies = savedCurrencies.tokens,
            )

            val newCurrencies = (newCoins + filteredCurrencies).distinct()

            val updatedResponse = savedCurrencies.copy(
                tokens = savedCurrencies.tokens + newCurrencies.map(userTokensResponseFactory::createResponseToken),
            )
            storeAndPushTokens(
                userWalletId = userWalletId,
                response = updatedResponse,
            )
            fetchExchangeableUserMarketCoinsByIds(userWalletId, updatedResponse)
        }
    }

    private suspend fun createCoinsForNewTokenList(
        userWalletId: UserWalletId,
        newTokens: List<CryptoCurrency.Token>,
    ): List<CryptoCurrency.Coin> {
        return newTokens.mapNotNull {
            cryptoCurrencyFactory.createCoin(
                blockchain = getBlockchain(networkId = it.network.id),
                extraDerivationPath = it.network.derivationPath.value,
                scanResponse = getUserWallet(userWalletId).scanResponse,
            )
        }.distinct()
    }

    private suspend fun createCoinsForNewTokens(
        userWalletId: UserWalletId,
        newTokens: List<CryptoCurrency.Token>,
        savedCurrencies: List<UserTokensResponse.Token>,
    ): List<CryptoCurrency.Coin> {
        return newTokens
            .filterNot { savedCurrencies.hasCoinForToken(it.network) } // tokens without coins
            .mapNotNull {
                cryptoCurrencyFactory.createCoin(
                    blockchain = getBlockchain(networkId = it.network.id),
                    extraDerivationPath = it.network.derivationPath.value,
                    scanResponse = getUserWallet(userWalletId).scanResponse,
                )
            }
            .distinct()
    }

    override suspend fun removeCurrency(userWalletId: UserWalletId, currency: CryptoCurrency) =
        withContext(dispatchers.io) {
            val savedCurrencies = requireNotNull(
                value = getSavedUserTokensResponseSync(key = userWalletId),
                lazyMessage = { "Saved tokens empty. Can not perform remove currency action" },
            )

            val token = userTokensResponseFactory.createResponseToken(currency)
            storeAndPushTokens(
                userWalletId = userWalletId,
                response = savedCurrencies.copy(tokens = savedCurrencies.tokens.filterNot { it == token }),
            )
        }

    override suspend fun removeCurrencies(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        return withContext(dispatchers.io) {
            val savedCurrencies = requireNotNull(
                value = getSavedUserTokensResponseSync(key = userWalletId),
                lazyMessage = { "Saved tokens empty. Can not perform remove currencies action" },
            )

            val tokens = currencies.map(userTokensResponseFactory::createResponseToken)
            storeAndPushTokens(
                userWalletId = userWalletId,
                response = savedCurrencies.copy(
                    tokens = savedCurrencies.tokens.filterNot(tokens::contains),
                ),
            )
        }
    }

    override fun getWalletCurrenciesUpdates(userWalletId: UserWalletId): Flow<List<CryptoCurrency>> {
        return channelFlow {
            val userWallet = getUserWallet(userWalletId)

            if (userWallet.isMultiCurrency) {
                getMultiCurrencyWalletCurrenciesUpdates(userWalletId).collect(::send)
            } else {
                val currencies = getSingleCurrencyWalletWithCardCurrencies(userWalletId)

                send(currencies)
            }
        }
    }

    override suspend fun getSingleCurrencyWalletPrimaryCurrency(userWalletId: UserWalletId): CryptoCurrency {
        return withContext(dispatchers.io) {
            val userWallet = getUserWallet(userWalletId)
            ensureIsCorrectUserWallet(userWallet, isMultiCurrencyWalletExpected = false)

            cardCurrenciesFactory.createPrimaryCurrencyForSingleCurrencyCard(userWallet.scanResponse)
        }
    }

    override suspend fun getSingleCurrencyWalletWithCardCurrencies(userWalletId: UserWalletId): List<CryptoCurrency> {
        return withContext(dispatchers.io) {
            val userWallet = getUserWallet(userWalletId)
            ensureIsCorrectUserWallet(userWallet, isMultiCurrencyWalletExpected = false)

            cardCurrenciesFactory.createCurrenciesForSingleCurrencyCardWithToken(userWallet.scanResponse)
        }
    }

    override suspend fun getSingleCurrencyWalletWithCardCurrency(
        userWalletId: UserWalletId,
        id: CryptoCurrency.ID,
    ): CryptoCurrency {
        return withContext(dispatchers.io) {
            val userWallet = getUserWallet(userWalletId)
            ensureIsCorrectUserWallet(userWallet, isMultiCurrencyWalletExpected = false)

            val currency = cardCurrenciesFactory.createCurrenciesForSingleCurrencyCardWithToken(userWallet.scanResponse)
                .find { it.id == id }
            requireNotNull(currency) { "Unable to find currency with provided ID: $id" }
        }
    }

    override fun getMultiCurrencyWalletCurrenciesUpdates(userWalletId: UserWalletId): Flow<List<CryptoCurrency>> {
        return channelFlow {
            val userWallet = getUserWallet(userWalletId)
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
        val userWallet = getUserWallet(userWalletId)
        ensureIsCorrectUserWallet(userWallet, isMultiCurrencyWalletExpected = true)

        fetchTokensIfCacheExpired(userWallet, refresh)

        val storedTokens = requireNotNull(
            value = getSavedUserTokensResponseSync(key = userWallet.walletId),
            lazyMessage = {
                "Unable to find tokens response for user wallet with provided ID: $userWalletId"
            },
        )

        responseCurrenciesFactory.createCurrencies(storedTokens, userWallet.scanResponse)
    }

    override suspend fun getMultiCurrencyWalletCachedCurrenciesSync(userWalletId: UserWalletId) =
        withContext(dispatchers.io) {
            val userWallet = getUserWallet(userWalletId)
            ensureIsCorrectUserWallet(userWallet, isMultiCurrencyWalletExpected = true)

            val storedTokens = requireNotNull(
                value = getSavedUserTokensResponseSync(key = userWallet.walletId),
                lazyMessage = {
                    "Unable to find tokens response for user wallet with provided ID: $userWalletId"
                },
            )

            responseCurrenciesFactory.createCurrencies(storedTokens, userWallet.scanResponse)
        }

    override suspend fun getMultiCurrencyWalletCurrency(
        userWalletId: UserWalletId,
        id: CryptoCurrency.ID,
    ): CryptoCurrency = withContext(dispatchers.io) {
        val userWallet = getUserWallet(userWalletId)
        ensureIsCorrectUserWallet(userWallet, isMultiCurrencyWalletExpected = true)

        val response = requireNotNull(
            value = getSavedUserTokensResponseSync(key = userWalletId),
            lazyMessage = {
                "Unable to find tokens response for user wallet with provided ID: $userWalletId"
            },
        )

        responseCurrenciesFactory.createCurrency(
            currencyId = id,
            response = response,
            scanResponse = userWallet.scanResponse,
        )
    }

    override suspend fun getNetworkCoin(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
    ): CryptoCurrency.Coin {
        return withContext(dispatchers.io) {
            val userWallet = getUserWallet(userWalletId)
            ensureIsCorrectUserWallet(userWallet = userWallet, isMultiCurrencyWalletExpected = true)

            fetchTokensIfCacheExpired(userWallet = userWallet, refresh = false)

            val storedTokens = requireNotNull(
                value = getSavedUserTokensResponseSync(key = userWalletId),
                lazyMessage = {
                    "Unable to find tokens response for user wallet with provided ID: $userWalletId"
                },
            )
            val blockchain = Blockchain.fromId(networkId.value)
            val blockchainNetworkId = blockchain.toNetworkId()
            val coinId = blockchain.toCoinId()

            val storedCoin = storedTokens.tokens
                .find {
                    it.networkId == blockchainNetworkId && it.id == coinId && it.derivationPath == derivationPath.value
                } ?: error("Coin in this network $networkId not found")

            val coin = responseCurrenciesFactory.createCurrency(storedCoin, userWallet.scanResponse)

            coin as? CryptoCurrency.Coin ?: error("Unable to create currency")
        }
    }

    override fun isTokensGrouped(userWalletId: UserWalletId): Flow<Boolean> {
        return channelFlow {
            val userWallet = getUserWallet(userWalletId)

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
            val userWallet = getUserWallet(userWalletId)

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

    override fun isSendBlockedByPendingTransactions(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        coinStatus: CryptoCurrencyStatus?,
    ): Boolean {
        val blockchain = Blockchain.fromId(cryptoCurrencyStatus.currency.network.id.value)
        val isBitcoinBlockchain = blockchain == Blockchain.Bitcoin || blockchain == Blockchain.BitcoinTestnet
        return when {
            cryptoCurrencyStatus.currency is CryptoCurrency.Coin && isBitcoinBlockchain -> {
                val outgoingTransactions = cryptoCurrencyStatus.value.pendingTransactions.filter { it.isOutgoing }
                outgoingTransactions.isNotEmpty()
            }
            blockchain.isEvm() -> false
            blockchain == Blockchain.Tron || blockchain == Blockchain.TronTestnet -> false
            else -> coinStatus?.value?.hasCurrentNetworkTransactions == true
        }
    }

    override suspend fun getFeePaidCurrency(userWalletId: UserWalletId, currency: CryptoCurrency): FeePaidCurrency {
        return withContext(dispatchers.io) {
            val blockchain = Blockchain.fromId(currency.network.id.value)
            when (val feePaidCurrency = blockchain.feePaidCurrency()) {
                FeePaidSdkCurrency.Coin -> FeePaidCurrency.Coin
                FeePaidSdkCurrency.SameCurrency -> FeePaidCurrency.SameCurrency
                is FeePaidSdkCurrency.Token -> {
                    val balance = walletManagersFacade.tokenBalance(
                        userWalletId = userWalletId,
                        network = currency.network,
                        name = feePaidCurrency.token.name,
                        symbol = feePaidCurrency.token.symbol,
                        contractAddress = feePaidCurrency.token.contractAddress,
                        decimals = feePaidCurrency.token.decimals,
                        id = feePaidCurrency.token.id,
                    )
                    FeePaidCurrency.Token(
                        tokenId = getTokenId(network = currency.network, sdkToken = feePaidCurrency.token),
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
        val userWallet = getUserWallet(userWalletId)
        val token = withContext(dispatchers.io) {
            val foundToken = tangemTechApi.getCoins(
                contractAddress = contractAddress,
                networkId = networkId,
            )
                .getOrThrow()
                .coins
                .firstOrNull()
                ?: error("Token not found")
            val network = foundToken.networks.firstOrNull { it.networkId == networkId } ?: error("Network not found")
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
            scanResponse = userWallet.scanResponse,
        ) ?: error("Unable to create token")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getAllWalletsCryptoCurrencies(currencyRawId: String): Flow<Map<UserWallet, List<CryptoCurrency>>> {
        return userWalletsStore.userWallets.flatMapLatest { userWallets ->
            userWallets.forEach { fetchTokensIfCacheExpired(userWallet = it, refresh = false) }

            val userWalletsWithCurrencies = userWallets
                .filterNot(UserWallet::isLocked)
                .map { userWallet ->
                    if (userWallet.isMultiCurrency) {
                        getSavedUserTokensResponse(userWallet.walletId).map { storedTokens ->
                            val filterResponse = storedTokens.tokens.filter {
                                getL2CompatibilityTokenComparison(it, currencyRawId)
                            }

                            responseCurrenciesFactory.createCurrencies(
                                response = storedTokens.copy(tokens = filterResponse),
                                scanResponse = userWallet.scanResponse,
                            )
                        }
                    } else {
                        flow {
                            val currency = getSingleCurrencyWalletPrimaryCurrency(userWalletId = userWallet.walletId)

                            val currencies = if (currency.id.rawCurrencyId == currencyRawId) {
                                listOf(currency)
                            } else {
                                emptyList()
                            }

                            emit(currencies)
                        }
                    }
                        .map { userWallet to it }
                }

            combine(userWalletsWithCurrencies) { it.toMap() }
                .onEmpty { emit(value = emptyMap()) }
        }
    }

    private fun getMultiCurrencyWalletCurrencies(userWallet: UserWallet): Flow<List<CryptoCurrency>> {
        return getSavedUserTokensResponse(userWallet.walletId).map { storedTokens ->
            responseCurrenciesFactory.createCurrencies(
                response = storedTokens,
                scanResponse = userWallet.scanResponse,
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

    private suspend fun fetchTokens(userWallet: UserWallet) {
        val userWalletId = userWallet.walletId

        val response = if (checkIsEmptyDemoWallet(userWallet)) {
            createDefaultUserTokensResponse(userWallet)
        } else {
            safeApiCall({ tangemTechApi.getUserTokens(userWalletId.stringValue).bind() }) {
                handleFetchTokensError(userWallet, it)
            }
        }

        val compatibleUserTokensResponse = response
            .let { it.copy(tokens = it.tokens.distinct()) }
            .let { customTokensMerger.mergeIfPresented(userWalletId, response) }
            .let(userTokensBackwardCompatibility::applyCompatibilityAndGetUpdated)

        appPreferencesStore.storeObject(
            key = PreferencesKeys.getUserTokensKey(userWalletId = userWallet.walletId.stringValue),
            value = compatibleUserTokensResponse,
        )

        fetchExchangeableUserMarketCoinsByIds(userWalletId, compatibleUserTokensResponse)
    }

    private suspend fun checkIsEmptyDemoWallet(userWallet: UserWallet): Boolean {
        val response = getSavedUserTokensResponseSync(key = userWallet.walletId)

        return demoConfig.isDemoCardId(userWallet.cardId) && response == null
    }

    private suspend fun storeAndPushTokens(userWalletId: UserWalletId, response: UserTokensResponse) {
        val compatibleUserTokensResponse = userTokensBackwardCompatibility.applyCompatibilityAndGetUpdated(response)
        appPreferencesStore.storeObject(
            key = PreferencesKeys.getUserTokensKey(userWalletId = userWalletId.stringValue),
            value = compatibleUserTokensResponse,
        )

        pushTokens(userWalletId, response)
    }

    private suspend fun fetchExchangeableUserMarketCoinsByIds(
        userWalletId: UserWalletId,
        userTokens: UserTokensResponse,
    ) {
        try {
            val tokensList = userTokens.tokens
                .map {
                    LeastTokenInfo(
                        contractAddress = it.contractAddress ?: EMPTY_CONTRACT_ADDRESS_VALUE,
                        network = it.networkId,
                    )
                }

            if (tokensList.isNotEmpty()) {
                val response = tangemExpressApi.getAssets(
                    AssetsRequestBody(
                        tokensList = tokensList,
                    ),
                )

                expressAssetsStore.store(userWalletId, response.getOrThrow())
            }
        } catch (e: Throwable) {
            Timber.e(e, "Unable to fetch assets for: ${userWalletId.stringValue}")
        }
    }

    private suspend fun handleFetchTokensError(userWallet: UserWallet, e: ApiResponseError): UserTokensResponse {
        val userWalletId = userWallet.walletId
        val response = appPreferencesStore.getObjectSyncOrNull(
            key = PreferencesKeys.getUserTokensKey(userWalletId.stringValue),
        ) ?: createDefaultUserTokensResponse(userWallet)

        if (e is ApiResponseError.HttpException && e.code == ApiResponseError.HttpException.Code.NOT_FOUND) {
            Timber.w(e, "Requested currencies could not be found in the remote store for: $userWalletId")

            pushTokens(userWalletId, response)
        } else {
            cacheRegistry.invalidate(getTokensCacheKey(userWalletId))
        }

        return response
    }

    private suspend fun pushTokens(userWalletId: UserWalletId, response: UserTokensResponse) {
        safeApiCall({ tangemTechApi.saveUserTokens(userWalletId.stringValue, response).bind() }) {
            Timber.e(it, "Unable to save user tokens for: ${userWalletId.stringValue}")
        }
    }

    private fun createDefaultUserTokensResponse(userWallet: UserWallet) =
        userTokensResponseFactory.createUserTokensResponse(
            currencies = cardCurrenciesFactory.createDefaultCoinsForMultiCurrencyCard(userWallet.scanResponse),
            isGroupedByNetwork = false,
            isSortedByBalance = false,
        )

    private suspend fun getUserWallet(userWalletId: UserWalletId): UserWallet {
        return requireNotNull(userWalletsStore.getSyncOrNull(userWalletId)) {
            "Unable to find a user wallet with provided ID: $userWalletId"
        }
    }

    private suspend fun ensureIsCorrectUserWallet(userWalletId: UserWalletId, isMultiCurrencyWalletExpected: Boolean) {
        val userWallet = getUserWallet(userWalletId)

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
        return appPreferencesStore
            .getObject<UserTokensResponse>(PreferencesKeys.getUserTokensKey(userWalletId = key.stringValue))
            .filterNotNull()
    }

    private suspend fun getSavedUserTokensResponseSync(key: UserWalletId): UserTokensResponse? {
        return appPreferencesStore.getObjectSyncOrNull<UserTokensResponse>(
            key = PreferencesKeys.getUserTokensKey(key.stringValue),
        )
    }
}
