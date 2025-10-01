package com.tangem.data.account.fetcher

import com.tangem.data.account.store.AccountsResponseStore
import com.tangem.data.account.store.AccountsResponseStoreFactory
import com.tangem.data.account.utils.assignTokens
import com.tangem.data.account.utils.toUserTokensResponse
import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.data.common.account.WalletAccountsSaver
import com.tangem.data.common.api.safeApiCall
import com.tangem.data.common.cache.etag.ETagsStore
import com.tangem.data.common.currency.UserTokensSaver
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError.HttpException.Code
import com.tangem.datasource.api.common.response.IF_NONE_MATCH_HEADER
import com.tangem.datasource.api.common.response.isNetworkError
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.SaveWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.datasource.utils.getSyncOrNull
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of [WalletAccountsFetcher] and [WalletAccountsSaver]
 *
 * @property tangemTechApi                   API for network requests
 * @property accountsResponseStoreFactory    factory to create [AccountsResponseStore]
 * @property userTokensSaver                 saves user tokens to the database
 * @property fetchWalletAccountsErrorHandler handles errors during fetching wallet accounts
 * @property eTagsStore                      store for ETags to manage caching
 * @property dispatchers                     dispatchers
 *
[REDACTED_AUTHOR]
 */
@Singleton
internal class DefaultWalletAccountsFetcher @Inject constructor(
    private val tangemTechApi: TangemTechApi,
    private val accountsResponseStoreFactory: AccountsResponseStoreFactory,
    private val userTokensSaver: UserTokensSaver,
    private val fetchWalletAccountsErrorHandler: FetchWalletAccountsErrorHandler,
    private val eTagsStore: ETagsStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : WalletAccountsFetcher, WalletAccountsSaver {

    override suspend fun fetch(userWalletId: UserWalletId) {
        val savedAccountsResponse = getAccountsResponseStore(userWalletId = userWalletId).getSyncOrNull()
        val accountsResponse = fetchWalletAccounts(userWalletId, savedAccountsResponse)
        val unassignedTokens = accountsResponse?.unassignedTokens

        if (!unassignedTokens.isNullOrEmpty()) {
            assignTokens(userWalletId, accountsResponse)
        }
    }

    override suspend fun pushAndStore(userWalletId: UserWalletId, response: GetWalletAccountsResponse) {
        push(userWalletId = userWalletId, accounts = response.accounts)
        store(userWalletId = userWalletId, response = response)
    }

    override suspend fun store(userWalletId: UserWalletId, response: GetWalletAccountsResponse) {
        val store = getAccountsResponseStore(userWalletId = userWalletId)

        store.updateData { response }
    }

    override suspend fun push(userWalletId: UserWalletId, accounts: List<WalletAccountDTO>) {
        push(userWalletId = userWalletId, body = SaveWalletAccountsResponse(accounts = accounts))
    }

    override suspend fun push(userWalletId: UserWalletId, body: SaveWalletAccountsResponse) {
        safeApiCall(
            call = {
                var eTag = getETag(userWalletId)

                if (eTag == null) {
                    fetch(userWalletId)

                    eTag = getETag(userWalletId) ?: error("ETag is null after fetch")
                }

                val apiResponse = withContext(dispatchers.io) {
                    tangemTechApi.saveWalletAccounts(
                        walletId = userWalletId.stringValue,
                        eTag = eTag,
                        body = body,
                    )
                }

                saveETag(userWalletId, apiResponse)

                apiResponse.bind()
            },
            onError = { error ->
                if (error.isNetworkError(code = Code.PRECONDITION_FAILED)) {
                    throw error
                }
            },
        )
    }

    private suspend fun fetchWalletAccounts(
        userWalletId: UserWalletId,
        savedAccountsResponse: GetWalletAccountsResponse?,
    ): GetWalletAccountsResponse? {
        return safeApiCall(
            call = {
                val apiResponse = withContext(dispatchers.io) {
                    tangemTechApi.getWalletAccounts(
                        walletId = userWalletId.stringValue,
                        eTag = getETag(userWalletId),
                    )
                }

                saveETag(userWalletId, apiResponse)

                val responseBody = apiResponse.bind()
                store(userWalletId = userWalletId, response = responseBody)

                responseBody
            },
            onError = {
                // pushWalletAccounts and storeWalletAccounts help to avoid cyclic dependency
                fetchWalletAccountsErrorHandler.handle(
                    error = it,
                    userWalletId = userWalletId,
                    savedAccountsResponse = savedAccountsResponse,
                    pushWalletAccounts = ::push,
                    storeWalletAccounts = ::store,
                )

                null
            },
        )
    }

    private suspend fun assignTokens(userWalletId: UserWalletId, accountsResponse: GetWalletAccountsResponse) {
        val accountsResponseWithTokens = accountsResponse.assignTokens(userWalletId)

        pushAndStore(userWalletId = userWalletId, response = accountsResponseWithTokens)

        userTokensSaver.push(
            userWalletId = userWalletId,
            response = accountsResponseWithTokens.toUserTokensResponse(),
        )
    }

    private suspend fun getETag(userWalletId: UserWalletId): String? {
        return eTagsStore.getSyncOrNull(userWalletId = userWalletId, key = ETagsStore.Key.WalletAccounts)
    }

    private suspend fun saveETag(userWalletId: UserWalletId, apiResponse: ApiResponse<*>) {
        val eTag = apiResponse.headers[IF_NONE_MATCH_HEADER]?.firstOrNull()

        if (eTag != null) {
            eTagsStore.store(userWalletId = userWalletId, key = ETagsStore.Key.WalletAccounts, value = eTag)
        }
    }

    private fun getAccountsResponseStore(userWalletId: UserWalletId): AccountsResponseStore {
        return accountsResponseStoreFactory.create(userWalletId = userWalletId)
    }
}