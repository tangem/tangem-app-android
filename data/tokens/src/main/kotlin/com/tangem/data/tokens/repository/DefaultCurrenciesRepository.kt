package com.tangem.data.tokens.repository

import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.tokens.utils.*
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.local.token.UserMarketCoinsStore
import com.tangem.datasource.local.token.UserTokensStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.core.error.DataError
import com.tangem.domain.demo.DemoConfig
import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val responseCurrenciesFactory = ResponseCurrenciesFactory(demoConfig)
    private val cardCurrenciesFactory = CardCurrenciesFactory(demoConfig)
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
                    derivationStyleProvider = getUserWallet(userWalletId).scanResponse.derivationStyleProvider,
                )
            }
    }

    private fun List<UserTokensResponse.Token>.hasCoinForToken(token: CryptoCurrency.Token): Boolean {
        return any {
            val blockchain = getBlockchain(networkId = token.network.id)

            it.id == getCoinId(blockchain).rawCurrencyId
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

        return responseCurrenciesFactory.createCurrencies(
            response = storedTokens,
            card = userWallet.scanResponse.card,
        )
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

        responseCurrenciesFactory.createCurrency(id, response, userWallet.scanResponse.card)
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
                card = userWallet.scanResponse.card,
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
        try {
            val response = tangemTechApi.getUserTokens(userWallet.walletId.stringValue)

            userTokensStore.store(userWallet.walletId, response)
            fetchUserMarketCoinsByIds(userWallet.walletId, response)
        } catch (e: Throwable) {
            handleFetchTokensErrorOrThrow(userWallet, e)
        }
    }

    private suspend fun storeAndPushTokens(userWalletId: UserWalletId, response: UserTokensResponse) {
        userTokensStore.store(userWalletId, response)
        tangemTechApi.saveUserTokens(userWalletId.stringValue, response)
    }

    private suspend fun fetchUserMarketCoinsByIds(userWalletId: UserWalletId, userTokens: UserTokensResponse) {
        try {
            val response = tangemTechApi.getCoins(
                networkIds = userTokens.tokens.joinToString(separator = ",") { it.networkId },
            )
            userMarketCoinsStore.store(userWalletId, response)
        } catch (e: Throwable) {
            Timber.e("Unable to fetch user market coins for: ${userWalletId.stringValue} ${e.message}")
        }
    }

    private suspend fun handleFetchTokensErrorOrThrow(userWallet: UserWallet, error: Throwable) {
        val errorMessage = error.message ?: throw error

        if (NOT_FOUND_HTTP_CODE in errorMessage) {
            val response = userTokensStore.getSyncOrNull(userWallet.walletId)
                ?: userTokensResponseFactory.createUserTokensResponse(
                    currencies = cardCurrenciesFactory.createDefaultCoinsForMultiCurrencyCard(
                        card = userWallet.scanResponse.card,
                        derivationStyleProvider = userWallet.scanResponse.derivationStyleProvider,
                    ),
                    isGroupedByNetwork = false,
                    isSortedByBalance = false,
                )

            tangemTechApi.saveUserTokens(userWallet.walletId.stringValue, response)
        } else {
            Timber.e(error, "Unable to fetch currencies for: ${userWallet.walletId}")
        }
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
        const val NOT_FOUND_HTTP_CODE = "404"
    }
}
