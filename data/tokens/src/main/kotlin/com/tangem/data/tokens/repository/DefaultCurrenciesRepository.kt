package com.tangem.data.tokens.repository

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.compatibility.getL2CompatibilityTokenComparison
import com.tangem.blockchainsdk.utils.*
import com.tangem.data.common.api.safeApiCall
import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.common.currency.*
import com.tangem.data.tokens.utils.CardCryptoCurrenciesFactory
import com.tangem.data.tokens.utils.CustomTokensMerger
import com.tangem.data.tokens.utils.UserTokensBackwardCompatibility
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.express.models.TangemExpressValues.EMPTY_CONTRACT_ADDRESS_VALUE
import com.tangem.datasource.api.express.models.request.LeastTokenInfo
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.exchangeservice.swap.ExpressServiceLoader
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObject
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrNull
import com.tangem.datasource.local.preferences.utils.storeObject
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.common.extensions.canHandleBlockchain
import com.tangem.domain.common.util.cardTypesResolver
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
import com.tangem.utils.extensions.filterIf
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
    private val appPreferencesStore: AppPreferencesStore,
    private val expressServiceLoader: ExpressServiceLoader,
    private val dispatchers: CoroutineDispatcherProvider,
    private val excludedBlockchains: ExcludedBlockchains,
) : CurrenciesRepository {

    private val demoConfig = DemoConfig()
    private val responseCurrenciesFactory = ResponseCryptoCurrenciesFactory(excludedBlockchains)
    private val cryptoCurrencyFactory = CryptoCurrencyFactory(excludedBlockchains)
    private val cardCurrenciesFactory = CardCryptoCurrenciesFactory(demoConfig, excludedBlockchains)
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
        withContext(dispatchers.io) {
            val savedResponse = requireNotNull(
                value = getSavedUserTokensResponseSync(key = userWalletId),
                lazyMessage = { "Saved tokens empty. Can not perform add currencies action." },
            )

            val newCurrencies = populateCurrenciesWithMissedCoins(currencies)

            val updatedResponse = savedResponse.copy(
                tokens = newCurrencies.map(userTokensResponseFactory::createResponseToken),
            )
            storeAndPushTokens(
                userWalletId = userWalletId,
                response = updatedResponse,
            )

            fetchExpressAssetsByNetworkIds(userWalletId, updatedResponse)
        }
    }

    override suspend fun addCurrencies(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        withContext(dispatchers.io) {
            val savedCurrencies = requireNotNull(
                value = getSavedUserTokensResponseSync(key = userWalletId),
                lazyMessage = { "Saved tokens empty. Can not perform add currencies action" },
            )

            val currenciesToAdd = populateCurrenciesWithMissedCoins(
                currencies = currencies,
            ).let {
                filterAlreadyAddedCurrencies(savedCurrencies.tokens, it)
            }
            val updatedResponse = savedCurrencies.copy(
                tokens = savedCurrencies.tokens + currenciesToAdd.map(userTokensResponseFactory::createResponseToken),
            )
            storeAndPushTokens(
                userWalletId = userWalletId,
                response = updatedResponse,
            )
            fetchExpressAssetsByNetworkIds(userWalletId, updatedResponse)
        }
    }

    private fun filterAlreadyAddedCurrencies(
        savedCurrencies: List<UserTokensResponse.Token>,
        currenciesToAdd: List<CryptoCurrency>,
    ): List<CryptoCurrency> {
        return currenciesToAdd.filter { currency ->
            val blockchain = getBlockchain(networkId = currency.network.id)
            val networkId = blockchain.toNetworkId()
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

    override suspend fun getSingleCurrencyWalletPrimaryCurrency(
        userWalletId: UserWalletId,
        refresh: Boolean,
    ): CryptoCurrency {
        return withContext(dispatchers.io) {
            val userWallet = getUserWallet(userWalletId)
            ensureIsCorrectUserWallet(userWallet, isMultiCurrencyWalletExpected = false)

            val currency = cardCurrenciesFactory.createPrimaryCurrencyForSingleCurrencyCard(userWallet.scanResponse)
            fetchExpressAssetsByNetworkIds(userWalletId, listOf(currency), refresh)
            currency
        }
    }

    override suspend fun getSingleCurrencyWalletWithCardCurrencies(
        userWalletId: UserWalletId,
        refresh: Boolean,
    ): List<CryptoCurrency> {
        return withContext(dispatchers.io) {
            val userWallet = getUserWallet(userWalletId)
            ensureIsCorrectUserWallet(userWallet, isMultiCurrencyWalletExpected = false)

            val currencies = cardCurrenciesFactory.createCurrenciesForSingleCurrencyCardWithToken(
                userWallet.scanResponse,
            )
            fetchExpressAssetsByNetworkIds(userWalletId, currencies, refresh)
            currencies
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
            fetchExpressAssetsByNetworkIds(userWalletId, listOf(currency))
            currency
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
        getMultiCurrencyWalletCurrency(userWalletId, id.value)
    }

    override suspend fun getMultiCurrencyWalletCurrency(userWalletId: UserWalletId, id: String): CryptoCurrency =
        withContext(dispatchers.io) {
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
                    it.networkId == blockchainNetworkId &&
                        compareIdWithMigrations(it, coinId) &&
                        it.derivationPath == derivationPath.value
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

    override suspend fun getFeePaidCurrency(userWalletId: UserWalletId, network: Network): FeePaidCurrency {
        return withContext(dispatchers.io) {
            val blockchain = Blockchain.fromId(network.id.value)
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
    override fun getAllWalletsCryptoCurrencies(
        currencyRawId: CryptoCurrency.RawID,
        needFilterByAvailable: Boolean,
    ): Flow<Map<UserWallet, List<CryptoCurrency>>> {
        return userWalletsStore.userWallets.flatMapLatest { userWallets ->
            userWallets.forEach { fetchTokensIfCacheExpired(userWallet = it, refresh = false) }

            val userWalletsWithCurrencies = userWallets
                .filterNot(UserWallet::isLocked)
                .filterIf(needFilterByAvailable) { it.filterWalletByAvailableBlockchain(currencyRawId) }
                .map { userWallet ->
                    if (userWallet.isMultiCurrency) {
                        getSavedUserTokensResponse(userWallet.walletId).map { storedTokens ->
                            val filterResponse = storedTokens.tokens.filter {
                                getL2CompatibilityTokenComparison(it, currencyRawId.value)
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

    private fun UserWallet.filterWalletByAvailableBlockchain(currencyRawId: CryptoCurrency.RawID): Boolean {
        val blockchain = Blockchain.fromNetworkId(currencyRawId.value) ?: return true
        return this.scanResponse.card.canHandleBlockchain(
            blockchain = blockchain,
            cardTypesResolver = this.cardTypesResolver,
            excludedBlockchains = excludedBlockchains,
        )
    }

    override fun isNetworkFeeZero(userWalletId: UserWalletId, network: Network): Boolean {
        val blockchain = Blockchain.fromNetworkId(network.backendId)
        return blockchain?.isNetworkFeeZero() ?: false
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

    private fun compareIdWithMigrations(token: UserTokensResponse.Token, coinId: String): Boolean {
        return when {
            token.id == OLD_POLYGON_NAME -> NEW_POLYGON_NAME == coinId
            else -> token.id == coinId
        }
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

        fetchExpressAssetsByNetworkIds(userWalletId, compatibleUserTokensResponse)
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

    private suspend fun fetchExpressAssetsByNetworkIds(userWalletId: UserWalletId, userTokens: UserTokensResponse) {
        val tokens = userTokens.tokens.map { token ->
            LeastTokenInfo(
                contractAddress = token.contractAddress ?: EMPTY_CONTRACT_ADDRESS_VALUE,
                network = token.networkId,
            )
        }

        coroutineScope {
            launch { expressServiceLoader.update(userWalletId, tokens) }
        }
    }

    private suspend fun fetchExpressAssetsByNetworkIds(
        userWalletId: UserWalletId,
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
            key = getAssetsCacheKey(userWalletId),
            skipCache = refresh,
            block = {
                coroutineScope {
                    launch { expressServiceLoader.update(userWalletId, tokens) }
                }
            },
        )
    }

    private fun getAssetsCacheKey(userWalletId: UserWalletId): String = "assets_cache_key_${userWalletId.stringValue}"

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