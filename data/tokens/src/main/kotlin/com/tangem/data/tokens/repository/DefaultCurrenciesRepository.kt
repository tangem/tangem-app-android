package com.tangem.data.tokens.repository

import com.tangem.blockchain.common.Blockchain
import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.tokens.utils.*
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.local.token.UserMarketCoinsStore
import com.tangem.datasource.local.token.UserTokensStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.common.extensions.toCoinId
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.core.error.DataError
import com.tangem.domain.demo.DemoConfig
import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.domain.tokens.models.Network
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import timber.log.Timber

internal class DefaultCurrenciesRepository(
    private val tangemTechApi: TangemTechApi,
    private val userTokensStore: UserTokensStore,
    private val userWalletsStore: UserWalletsStore,
    private val userMarketCoinsStore: UserMarketCoinsStore,
    private val cacheRegistry: CacheRegistry,
    private val dispatchers: CoroutineDispatcherProvider,
) : CurrenciesRepository {

    private val demoConfig = DemoConfig()
    private val responseCurrenciesFactory = ResponseCryptoCurrenciesFactory(demoConfig)
    private val cardCurrenciesFactory = CardCryptoCurrenciesFactory(demoConfig)
    private val userTokensResponseFactory = UserTokensResponseFactory()

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

    override suspend fun addCurrencies(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        return withContext(dispatchers.io) {
            val savedCurrencies = requireNotNull(
                value = userTokensStore.getSyncOrNull(userWalletId),
                lazyMessage = { "Saved tokens empty. Can not perform add currencies action" },
            )

            val newCoins = createCoinsForNewTokens(
                userWalletId = userWalletId,
                newTokens = currencies.filterIsInstance<CryptoCurrency.Token>(),
                savedCurrencies = savedCurrencies.tokens,
            )

            val newCurrencies = newCoins + currencies

            storeAndPushTokens(
                userWalletId = userWalletId,
                response = savedCurrencies.copy(
                    tokens = savedCurrencies.tokens + newCurrencies.map(userTokensResponseFactory::createResponseToken),
                ),
            )
        }
    }

    private suspend fun createCoinsForNewTokens(
        userWalletId: UserWalletId,
        newTokens: List<CryptoCurrency.Token>,
        savedCurrencies: List<UserTokensResponse.Token>,
    ): List<CryptoCurrency.Coin> {
        return newTokens
            .filterNot { savedCurrencies.hasCoinForToken(it) } // tokens without coins
            .mapNotNull {
                CryptoCurrencyFactory().createCoin(
                    blockchain = getBlockchain(networkId = it.network.id),
                    extraDerivationPath = it.network.derivationPath.value,
                    derivationStyleProvider = getUserWallet(userWalletId).scanResponse.derivationStyleProvider,
                )
            }
    }

    private fun List<UserTokensResponse.Token>.hasCoinForToken(token: CryptoCurrency.Token): Boolean {
        return any {
            val blockchain = getBlockchain(networkId = token.network.id)

            it.id == blockchain.toCoinId()
        }
    }

    override suspend fun removeCurrency(userWalletId: UserWalletId, currency: CryptoCurrency) =
        withContext(dispatchers.io) {
            val savedCurrencies = requireNotNull(
                value = userTokensStore.getSyncOrNull(userWalletId),
                lazyMessage = { "Saved tokens empty. Can not perform remove currency action" },
            )

            val token = userTokensResponseFactory.createResponseToken(currency)
            storeAndPushTokens(
                userWalletId = userWalletId,
                response = savedCurrencies.copy(
                    tokens = savedCurrencies.tokens.filter { it != token },
                ),
            )
        }

    override suspend fun removeCurrencies(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        return withContext(dispatchers.io) {
            val savedCurrencies = requireNotNull(
                value = userTokensStore.getSyncOrNull(userWalletId),
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

    override suspend fun getSingleCurrencyWalletPrimaryCurrency(userWalletId: UserWalletId): CryptoCurrency {
        return withContext(dispatchers.io) {
            val userWallet = getUserWallet(userWalletId)
            ensureIsCorrectUserWallet(userWallet, isMultiCurrencyWalletExpected = false)

            cardCurrenciesFactory.createPrimaryCurrencyForSingleCurrencyCard(userWallet.scanResponse)
        }
    }

    override fun getMultiCurrencyWalletCurrenciesUpdates(userWalletId: UserWalletId): Flow<List<CryptoCurrency>> {
        return channelFlow {
            val userWallet = getUserWallet(userWalletId)
            ensureIsCorrectUserWallet(userWallet, isMultiCurrencyWalletExpected = true)

            launch(dispatchers.io) {
                getMultiCurrencyWalletCurrencies(userWallet).collect(::send)
            }

            launch(dispatchers.io) {
                fetchTokensIfCacheExpired(userWallet, refresh = false)
            }
        }
    }

    override suspend fun getMultiCurrencyWalletCurrenciesSync(
        userWalletId: UserWalletId,
        refresh: Boolean,
    ): List<CryptoCurrency> {
        val userWallet = getUserWallet(userWalletId)
        ensureIsCorrectUserWallet(userWallet, isMultiCurrencyWalletExpected = true)

        fetchTokensIfCacheExpired(userWallet, refresh)

        val storedTokens = requireNotNull(userTokensStore.getSyncOrNull(userWallet.walletId)) {
            "Unable to find tokens response for user wallet with provided ID: $userWalletId"
        }

        return responseCurrenciesFactory.createCurrencies(storedTokens, userWallet.scanResponse)
    }

    override suspend fun getMultiCurrencyWalletCurrency(
        userWalletId: UserWalletId,
        id: CryptoCurrency.ID,
    ): CryptoCurrency = withContext(dispatchers.io) {
        val userWallet = getUserWallet(userWalletId)
        ensureIsCorrectUserWallet(userWallet, isMultiCurrencyWalletExpected = true)

        val response = requireNotNull(userTokensStore.getSyncOrNull(userWalletId)) {
            "Unable to find tokens response for user wallet with provided ID: $userWalletId"
        }

        responseCurrenciesFactory.createCurrency(id, response, userWallet.scanResponse)
    }

    override suspend fun getNetworkCoin(userWalletId: UserWalletId, networkId: Network.ID): CryptoCurrency.Coin {
        val userWallet = getUserWallet(userWalletId)
        ensureIsCorrectUserWallet(userWallet = userWallet, isMultiCurrencyWalletExpected = true)

        fetchTokensIfCacheExpired(userWallet = userWallet, refresh = false)

        val storedTokens = requireNotNull(userTokensStore.getSyncOrNull(userWallet.walletId)) {
            "Unable to find tokens response for user wallet with provided ID: $userWalletId"
        }

        val storedCoin = storedTokens.tokens.find { it.networkId == Blockchain.fromId(networkId.value).toNetworkId() }
            ?: error("Coin in this network $networkId not found")

        val coin = responseCurrenciesFactory.createCurrency(storedCoin, userWallet.scanResponse)

        return coin as? CryptoCurrency.Coin ?: error("Unable to create currency")
    }

    override fun isTokensGrouped(userWalletId: UserWalletId): Flow<Boolean> {
        return channelFlow {
            ensureIsCorrectUserWallet(userWalletId, isMultiCurrencyWalletExpected = true)

            launch(dispatchers.io) {
                userTokensStore.get(userWalletId)
                    .map { it.group == UserTokensResponse.GroupType.NETWORK }
                    .collect(::send)
            }
        }
    }

    override fun isTokensSortedByBalance(userWalletId: UserWalletId): Flow<Boolean> {
        return channelFlow {
            ensureIsCorrectUserWallet(userWalletId, isMultiCurrencyWalletExpected = true)

            launch(dispatchers.io) {
                userTokensStore.get(userWalletId)
                    .map { it.sort == UserTokensResponse.SortType.BALANCE }
                    .collect(::send)
            }
        }
    }

    private fun getMultiCurrencyWalletCurrencies(userWallet: UserWallet): Flow<List<CryptoCurrency>> {
        return userTokensStore.get(userWallet.walletId).map { storedTokens ->
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

        val response = try {
            with(tangemTechApi.getUserTokens(userWalletId.stringValue)) {
                // The response may contain repeated tokens
                copy(tokens = tokens.distinct())
            }
        } catch (e: HttpException) {
            handleCurrenciesNotFoundOrThrow(userWallet, e)
        }

        userTokensStore.store(userWallet.walletId, response)
        fetchUserMarketCoinsByIds(userWalletId, response)
    }

    private suspend fun storeAndPushTokens(userWalletId: UserWalletId, response: UserTokensResponse) {
        userTokensStore.store(userWalletId, response)
        tangemTechApi.saveUserTokens(userWalletId.stringValue, response)
    }

    private suspend fun fetchUserMarketCoinsByIds(userWalletId: UserWalletId, userTokens: UserTokensResponse) {
        try {
            val networkIds = userTokens.tokens.joinToString(separator = ",") { it.networkId }
            val response = tangemTechApi.getCoins(networkIds)

            userMarketCoinsStore.store(userWalletId, response)
        } catch (e: Throwable) {
            Timber.e(e, "Unable to fetch user market coins for: ${userWalletId.stringValue}")
        }
    }

    private suspend fun handleCurrenciesNotFoundOrThrow(
        userWallet: UserWallet,
        httpException: HttpException,
    ): UserTokensResponse {
        val userWalletId = userWallet.walletId

        if (httpException.code() != NOT_FOUND_HTTP_CODE) {
            Timber.e(httpException, "Unable to fetch currencies for: $userWalletId")
            throw httpException
        }

        Timber.d("Requested currencies could not be found in the remote store for: $userWalletId")

        val response = userTokensStore.getSyncOrNull(userWalletId)
            ?: userTokensResponseFactory.createUserTokensResponse(
                currencies = cardCurrenciesFactory.createDefaultCoinsForMultiCurrencyCard(userWallet.scanResponse),
                isGroupedByNetwork = false,
                isSortedByBalance = false,
            )

        tangemTechApi.saveUserTokens(userWalletId.stringValue, response)

        return response
    }

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

    private companion object {
        const val NOT_FOUND_HTTP_CODE = 404
    }
}
