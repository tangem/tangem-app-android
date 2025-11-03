package com.tangem.data.account.fetcher

import com.tangem.data.account.utils.DefaultWalletAccountsResponseFactory
import com.tangem.data.account.utils.toUserTokensResponse
import com.tangem.data.common.currency.UserTokensResponseAccountIdEnricher
import com.tangem.data.common.currency.UserTokensSaver
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.ApiResponseError.HttpException.Code
import com.tangem.datasource.api.common.response.isNetworkError
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.datasource.local.token.UserTokensResponseStore
import com.tangem.domain.models.wallet.UserWalletId
import timber.log.Timber
import javax.inject.Inject

/**
 * Handles errors that occur during the fetching of wallet accounts
 *
 * @property userTokensSaver                      saves user tokens to the storage
 * @property userTokensResponseStore              provides access to user token responses.
 * @property defaultWalletAccountsResponseFactory creates [GetWalletAccountsResponse] from [UserTokensResponse]
 *
 * @see DefaultWalletAccountsFetcher
 *
[REDACTED_AUTHOR]
 */
internal class FetchWalletAccountsErrorHandler @Inject constructor(
    private val userTokensSaver: UserTokensSaver,
    private val userTokensResponseStore: UserTokensResponseStore,
    private val defaultWalletAccountsResponseFactory: DefaultWalletAccountsResponseFactory,
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
    ): GetWalletAccountsResponse? {
        val isResponseUpToDate = error.isNetworkError(code = Code.NOT_MODIFIED)
        if (isResponseUpToDate) {
            Timber.e("ETag is up to date, no need to update accounts for wallet: $userWalletId")
            return savedAccountsResponse
        }

        var response = savedAccountsResponse ?: createDefaultResponse(userWalletId)
        val (accountDTOs, userTokensResponse) = response.accounts to response.toUserTokensResponse()

        val isNotFoundError = error.isNetworkError(code = Code.NOT_FOUND)
        if (isNotFoundError) {
            userTokensSaver.push(userWalletId = userWalletId, response = userTokensResponse)
            val updatedResponse = pushWalletAccounts(userWalletId, accountDTOs)

            if (updatedResponse != null) {
                response = updatedResponse
            }
        }

        storeWalletAccounts(userWalletId, response)

        return response
    }

    private suspend fun createDefaultResponse(userWalletId: UserWalletId): GetWalletAccountsResponse {
        return defaultWalletAccountsResponseFactory.create(
            userWalletId = userWalletId,
            userTokensResponse = getFromLegacyStore(userWalletId),
        )
    }

    private suspend fun getFromLegacyStore(userWalletId: UserWalletId): UserTokensResponse? {
        return userTokensResponseStore.getSyncOrNull(userWalletId)
            ?.let {
                it.copy(
                    tokens = UserTokensResponseAccountIdEnricher(userWalletId = userWalletId, tokens = it.tokens),
                )
            }
            .also { userTokensResponseStore.clear(userWalletId) }
    }
}