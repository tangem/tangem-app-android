package com.tangem.data.account.fetcher

import com.tangem.data.account.fetcher.DefaultWalletAccountsFetcher.FetchResult
import com.tangem.data.account.utils.DefaultWalletAccountsResponseFactory
import com.tangem.data.common.cache.etag.ETagsStore
import com.tangem.data.common.currency.UserTokensResponseAccountIdEnricher
import com.tangem.data.common.currency.UserTokensSaver
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.ApiResponseError.HttpException.Code
import com.tangem.datasource.api.common.response.ETAG_HEADER
import com.tangem.datasource.api.common.response.isNetworkError
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.converters.WalletIdBodyConverter
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.datasource.api.tangemTech.models.account.toUserTokensResponse
import com.tangem.datasource.local.token.UserTokensResponseStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles errors that occur during the fetching of wallet accounts
 *
 * @property tangemTechApi                        API for network requests
 * @property userTokensSaver                      saves user tokens to the storage
 * @property userTokensResponseStore              provides access to user token responses.
 * @property defaultWalletAccountsResponseFactory creates [GetWalletAccountsResponse] from [UserTokensResponse]
 * @property eTagsStore                           store for ETags to manage caching
 * @property dispatchers                          dispatchers
 *
 * @see DefaultWalletAccountsFetcher
 *
[REDACTED_AUTHOR]
 */
@Suppress("LongParameterList")
@Singleton
internal class FetchWalletAccountsErrorHandler @Inject constructor(
    private val tangemTechApi: TangemTechApi,
    private val userWalletsStore: UserWalletsStore,
    private val userTokensSaver: UserTokensSaver,
    private val userTokensResponseStore: UserTokensResponseStore,
    private val defaultWalletAccountsResponseFactory: DefaultWalletAccountsResponseFactory,
    private val eTagsStore: ETagsStore,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    /**
     * Handles the error that occurred during the fetching of wallet accounts.
     * [pushWalletAccounts] and [storeWalletAccounts] are functions that passed as parameters to avoid
     * cyclic dependencies.
     *
     * @param error                 the error that occurred
     * @param userWalletId          the ID of the user wallet
     * @param savedAccountsResponse the previously saved wallet accounts response, if available
     * @param pushWalletAccounts    function to push wallet accounts to the server
     * @param storeWalletAccounts   function to store wallet accounts locally
     */
    suspend fun handle(
        error: ApiResponseError,
        userWalletId: UserWalletId,
        savedAccountsResponse: GetWalletAccountsResponse?,
        pushWalletAccounts: suspend (UserWalletId, List<WalletAccountDTO>) -> GetWalletAccountsResponse?,
        storeWalletAccounts: suspend (UserWalletId, GetWalletAccountsResponse) -> Unit,
    ): FetchResult {
        val isResponseUpToDate = error.isNetworkError(code = Code.NOT_MODIFIED)
        if (isResponseUpToDate) {
            Timber.e("ETag is up to date, no need to update accounts for wallet: $userWalletId")
            val response = requireNotNull(savedAccountsResponse) {
                "Saved accounts response is null for wallet: $userWalletId"
            }

            return FetchResult(response)
        }

        val response = savedAccountsResponse ?: createDefaultResponse(userWalletId)
        val (accountDTOs, userTokensResponse) = response.accounts to response.toUserTokensResponse()

        val isNotFoundError = error.isNetworkError(code = Code.NOT_FOUND)
        if (isNotFoundError) {
            val eTag = createWallet(userWalletId)

            if (eTag != null) {
                eTagsStore.store(userWalletId = userWalletId, key = ETagsStore.Key.WalletAccounts, value = eTag)

                pushWalletAccounts(userWalletId, accountDTOs)
                userTokensSaver.pushWithRetryer(userWalletId, userTokensResponse)
            }
        }

        storeWalletAccounts(userWalletId, response)

        return FetchResult(response, error)
    }

    private suspend fun createDefaultResponse(userWalletId: UserWalletId): GetWalletAccountsResponse {
        return defaultWalletAccountsResponseFactory.create(
            userWalletId = userWalletId,
            userTokensResponse = getFromLegacyStore(userWalletId),
        )
    }

    private suspend fun getFromLegacyStore(userWalletId: UserWalletId): UserTokensResponse? {
        return userTokensResponseStore.getSyncOrNull(userWalletId)
            ?.let { response ->
                response.copy(
                    tokens = UserTokensResponseAccountIdEnricher(userWalletId = userWalletId, tokens = response.tokens),
                )
            }
            .also { userTokensResponseStore.clear(userWalletId) }
    }

    /**
     * Creates a wallet on the server and returns the ETag if successful.
     *
     * @param userWalletId The ID of the user wallet to create.

     */
    private suspend fun createWallet(userWalletId: UserWalletId): String? {
        val userWallet = userWalletsStore.getSyncOrNull(key = userWalletId) ?: return null

        val creationResponse = withContext(dispatchers.io) {
            tangemTechApi.createWallet(
                body = WalletIdBodyConverter.convert(userWallet),
            )
        }

        return if (creationResponse is ApiResponse.Success && creationResponse.code == Code.CREATED) {
            creationResponse.headers[ETAG_HEADER]?.firstOrNull()
        } else {
            null
        }
    }
}