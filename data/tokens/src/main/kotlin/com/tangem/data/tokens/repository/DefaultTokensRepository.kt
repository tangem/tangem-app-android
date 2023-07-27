package com.tangem.data.tokens.repository

import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.tokens.utils.CardTokensFactory
import com.tangem.data.tokens.utils.ResponseTokensFactory
import com.tangem.data.tokens.utils.UserTokensResponseFactory
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.local.token.UserTokensStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.demo.DemoConfig
import com.tangem.domain.tokens.model.Token
import com.tangem.domain.tokens.repository.TokensRepository
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class DefaultTokensRepository(
    private val tangemTechApi: TangemTechApi,
    private val userTokensStore: UserTokensStore,
    private val userWalletsStore: UserWalletsStore,
    private val cacheRegistry: CacheRegistry,
    private val dispatchers: CoroutineDispatcherProvider,
) : TokensRepository {

    private val demoConfig = DemoConfig()
    private val responseTokensFactory = ResponseTokensFactory(demoConfig)
    private val cardTokensFactory = CardTokensFactory(demoConfig)
    private val userTokensResponseFactory = UserTokensResponseFactory()

    override suspend fun saveTokens(
        userWalletId: UserWalletId,
        tokens: Set<Token>,
        isGroupedByNetwork: Boolean,
        isSortedByBalance: Boolean,
    ) = withContext(dispatchers.io) {
        val response = userTokensResponseFactory.createUserTokensResponse(
            tokens = tokens,
            isGroupedByNetwork = isGroupedByNetwork,
            isSortedByBalance = isSortedByBalance,
        )

        storeAndPushTokens(userWalletId, response)
    }

    override fun getTokens(userWalletId: UserWalletId, refresh: Boolean): Flow<Set<Token>> = channelFlow {
        val userWallet = withContext(dispatchers.io) {
            requireNotNull(userWalletsStore.getSyncOrNull(userWalletId)) {
                "Unable to find a user wallet with provided ID: $userWalletId"
            }
        }

        if (userWallet.isMultiCurrency) {
            launch(dispatchers.io) {
                getMultiCurrencyWalletTokens(userWallet).collectLatest(::send)
            }

            launch(dispatchers.io) {
                fetchTokensIfCacheExpired(userWallet, refresh)
            }
        } else {
            send(getSingleCurrencyWalletTokens(userWallet))
        }
    }

    override fun isTokensGrouped(userWalletId: UserWalletId): Flow<Boolean> {
        return userTokensStore.get(userWalletId)
            .map { it.group == UserTokensResponse.GroupType.NETWORK }
            .flowOn(dispatchers.io)
    }

    override fun isTokensSortedByBalance(userWalletId: UserWalletId): Flow<Boolean> {
        return userTokensStore.get(userWalletId)
            .map { it.sort == UserTokensResponse.SortType.BALANCE }
            .flowOn(dispatchers.io)
    }

    private fun getMultiCurrencyWalletTokens(userWallet: UserWallet): Flow<Set<Token>> {
        return userTokensStore.get(userWallet.walletId).map { storedTokens ->
            responseTokensFactory.createTokens(
                response = storedTokens,
                card = userWallet.scanResponse.card,
            )
        }
    }

    private suspend fun getSingleCurrencyWalletTokens(userWallet: UserWallet): Set<Token> {
        var response = userTokensStore.getSyncOrNull(userWallet.walletId)

        if (response == null) {
            response = userTokensResponseFactory.createUserTokensResponse(
                tokens = cardTokensFactory.createTokensForSingleCurrencyCard(userWallet.scanResponse),
                isGroupedByNetwork = false,
                isSortedByBalance = false,
            )

            userTokensStore.store(userWallet.walletId, response)
        }

        return responseTokensFactory.createTokens(response, userWallet.scanResponse.card)
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
        } catch (e: Throwable) {
            handleFetchTokensErrorOrThrow(userWallet, e)
        }
    }

    private suspend fun storeAndPushTokens(userWalletId: UserWalletId, response: UserTokensResponse) {
        userTokensStore.store(userWalletId, response)
        tangemTechApi.saveUserTokens(userWalletId.stringValue, response)
    }

    private suspend fun handleFetchTokensErrorOrThrow(userWallet: UserWallet, error: Throwable) {
        val errorMessage = error.message ?: throw error

        if (NOT_FOUND_HTTP_CODE in errorMessage) {
            val response = userTokensStore.getSyncOrNull(userWallet.walletId)
                ?: userTokensResponseFactory.createUserTokensResponse(
                    tokens = cardTokensFactory.createDefaultTokensForMultiCurrencyCard(userWallet.scanResponse.card),
                    isGroupedByNetwork = false,
                    isSortedByBalance = false,
                )

            tangemTechApi.saveUserTokens(userWallet.walletId.stringValue, response)
        } else {
            throw error
        }
    }

    private fun getTokensCacheKey(userWalletId: UserWalletId): String = "tokens_cache_key_${userWalletId.stringValue}"

    private companion object {
        const val NOT_FOUND_HTTP_CODE = "404"
    }
}
