package com.tangem.data.account.fetcher

import com.tangem.data.account.store.AccountsResponseStore
import com.tangem.data.account.store.AccountsResponseStoreFactory
import com.tangem.data.account.utils.DefaultWalletAccountsResponseFactory
import com.tangem.data.account.utils.assignTokens
import com.tangem.data.account.utils.toUserTokensResponse
import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.data.common.account.WalletAccountsSaver
import com.tangem.data.common.api.safeApiCall
import com.tangem.data.common.cache.etag.ETagsStore
import com.tangem.data.common.currency.UserTokensSaver
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError.HttpException.Code
import com.tangem.datasource.api.common.response.ETAG_HEADER
import com.tangem.datasource.api.common.response.isNetworkError
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
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
 * @property defaultWalletAccountsResponseFactory creates [GetWalletAccountsResponse] from [UserTokensResponse]
 * @property eTagsStore                      store for ETags to manage caching
 * @property dispatchers                     dispatchers
 *
[REDACTED_AUTHOR]
 */
@Suppress("LongParameterList")
@Singleton
internal class DefaultWalletAccountsFetcher @Inject constructor(
    private val tangemTechApi: TangemTechApi,
    private val accountsResponseStoreFactory: AccountsResponseStoreFactory,
    private val userTokensSaver: UserTokensSaver,
    private val fetchWalletAccountsErrorHandler: FetchWalletAccountsErrorHandler,
    private val defaultWalletAccountsResponseFactory: DefaultWalletAccountsResponseFactory,
    private val eTagsStore: ETagsStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : WalletAccountsFetcher, WalletAccountsSaver {

    override suspend fun fetch(userWalletId: UserWalletId) {
        val savedAccountsResponse = getAccountsResponseStore(userWalletId = userWalletId).getSyncOrNull()
        val accountsResponse = fetchWalletAccounts(userWalletId, savedAccountsResponse)
            ?: return

        if (accountsResponse.accounts.isEmpty()) {
            initializeAccounts(userWalletId, accountsResponse)
        } else if (accountsResponse.unassignedTokens.isNotEmpty()) {
            assignTokens(userWalletId, accountsResponse)
        }
    }

    override suspend fun store(userWalletId: UserWalletId, response: GetWalletAccountsResponse) {
        val store = getAccountsResponseStore(userWalletId = userWalletId)

        store.updateData { response }
    }

    override suspend fun push(
        userWalletId: UserWalletId,
        accounts: List<WalletAccountDTO>,
    ): GetWalletAccountsResponse? {
        return push(userWalletId = userWalletId, body = SaveWalletAccountsResponse(accounts = accounts))
    }

    override suspend fun push(
        userWalletId: UserWalletId,
        body: SaveWalletAccountsResponse,
    ): GetWalletAccountsResponse? {
        return safeApiCall(
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

                null
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
            onError = { throwable ->
                // pushWalletAccounts and storeWalletAccounts help to avoid cyclic dependency
                fetchWalletAccountsErrorHandler.handle(
                    error = throwable,
                    userWalletId = userWalletId,
                    savedAccountsResponse = savedAccountsResponse,
                    pushWalletAccounts = ::push,
                    storeWalletAccounts = ::store,
                )
            },
        )
    }

    private suspend fun initializeAccounts(userWalletId: UserWalletId, accountsResponse: GetWalletAccountsResponse) {
        val response = defaultWalletAccountsResponseFactory.create(
            userWalletId = userWalletId,
            userTokensResponse = UserTokensResponse(
                group = accountsResponse.wallet.group,
                sort = accountsResponse.wallet.sort,
                tokens = accountsResponse.unassignedTokens,
            ),
        )

        userTokensSaver.push(userWalletId = userWalletId, response = response.toUserTokensResponse())
        val syncedResponse = push(userWalletId = userWalletId, accounts = response.accounts)

        if (syncedResponse != null) {
            store(userWalletId = userWalletId, response = syncedResponse)
        }
    }

    private suspend fun assignTokens(userWalletId: UserWalletId, accountsResponse: GetWalletAccountsResponse) {
        val accountsResponseWithTokens = accountsResponse.assignTokens(userWalletId)

        store(userWalletId = userWalletId, response = accountsResponseWithTokens)

        userTokensSaver.push(
            userWalletId = userWalletId,
            response = accountsResponseWithTokens.toUserTokensResponse(),
        )
    }

    private suspend fun getETag(userWalletId: UserWalletId): String? {
        return eTagsStore.getSyncOrNull(userWalletId = userWalletId, key = ETagsStore.Key.WalletAccounts)
    }

    private suspend fun saveETag(userWalletId: UserWalletId, apiResponse: ApiResponse<*>) {
        val eTag = apiResponse.headers[ETAG_HEADER]?.firstOrNull()

        if (eTag != null) {
            eTagsStore.store(userWalletId = userWalletId, key = ETagsStore.Key.WalletAccounts, value = eTag)
        }
    }

    private fun getAccountsResponseStore(userWalletId: UserWalletId): AccountsResponseStore {
        return accountsResponseStoreFactory.create(userWalletId = userWalletId)
    }
}