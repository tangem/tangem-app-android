package com.tangem.data.common.currency

import com.tangem.data.common.api.safeApiCall
import com.tangem.data.common.cache.etag.WalletAccountsETags
import com.tangem.data.common.wallet.WalletServerBinder
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.datasource.api.common.response.isNetworkError
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncOrNull
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.jointaccount.JointAccountFeatureToggles
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import com.tangem.utils.retryer.Retryer
import com.tangem.utils.retryer.RetryerPool
import kotlinx.coroutines.withContext

@Suppress("LongParameterList")
class UserTokensSaver(
    private val tangemTechApi: TangemTechApi,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val dispatchers: CoroutineDispatcherProvider,
    private val addressesEnricher: UserTokensResponseAddressesEnricher,
    private val walletServerBinder: WalletServerBinder,
    private val pushTokensRetryerPool: RetryerPool,
    private val walletAccountsETags: WalletAccountsETags,
    private val jointAccountFeatureToggles: JointAccountFeatureToggles,
) {

    suspend fun push(
        userWalletId: UserWalletId,
        response: UserTokensResponse,
        useEnricher: Boolean = true,
        onFailSend: () -> Unit = {},
    ) = withContext(dispatchers.io) {
        val userWallet = userWalletsListRepository.getSyncOrNull(id = userWalletId)

        if (userWallet == null) {
            TangemLogger.e("UserWallet with id $userWalletId not found. Cannot push tokens.")
            onFailSend()
            return@withContext
        }

        val enrichedResponse = response
            .withoutDuplicates()
            .enrichIf(userWalletId = userWalletId, condition = useEnricher)

        push(userWallet = userWallet, response = enrichedResponse, onFailSend = onFailSend)
    }

    suspend fun pushWithRetryer(
        userWalletId: UserWalletId,
        response: UserTokensResponse,
        useEnricher: Boolean = true,
        onFailSend: () -> Unit = {},
    ) {
        push(
            userWalletId = userWalletId,
            response = response,
            useEnricher = useEnricher,
            onFailSend = {
                pushTokensRetryerPool + createPushTokensRetryer(userWalletId, response)
                onFailSend()
            },
        )
    }

    private suspend fun push(userWallet: UserWallet, response: UserTokensResponse, onFailSend: () -> Unit) {
        safeApiCall(
            call = {
                // Both codes are expected flow, not failures, so they are handled before bind() gets to log them
                var apiResponse = saveTokensWithETag(userWalletId = userWallet.walletId, response = response)

                if (apiResponse.isError(code = ApiResponseError.HttpException.Code.NOT_FOUND)) {
                    walletServerBinder.bind(userWallet).bind()

                    apiResponse = saveTokensWithETag(userWalletId = userWallet.walletId, response = response)
                }

                // The gate is belt and braces: without the toggle no If-Match is sent, so a 412 cannot arrive
                val isRecoverableStaleWrite = jointAccountFeatureToggles.isJointAccountEnabled &&
                    apiResponse.isError(code = ApiResponseError.HttpException.Code.PRECONDITION_FAILED)

                if (isRecoverableStaleWrite) {
                    mergeAndRetry(userWalletId = userWallet.walletId, response = response, onFailSend = onFailSend)
                } else {
                    apiResponse.bind()
                }
            },
            onError = { error ->
                TangemLogger.e("Failed to push ${response.tokens.size} user tokens", error)
                onFailSend()
            },
        )
    }

    private fun ApiResponse<*>.isError(code: ApiResponseError.HttpException.Code): Boolean {
        return this is ApiResponse.Error && cause.isNetworkError(code)
    }

    private suspend fun saveTokensWithETag(
        userWalletId: UserWalletId,
        response: UserTokensResponse,
    ): ApiResponse<Unit> {
        val apiResponse = tangemTechApi.saveTokens(
            userId = userWalletId.stringValue,
            userTokens = response,
            eTag = getETagOrNull(userWalletId = userWalletId),
        )

        if (apiResponse is ApiResponse.Success) {
            refreshETag(userWalletId = userWalletId, apiResponse = apiResponse)
        }

        return apiResponse
    }

    private suspend fun mergeAndRetry(
        userWalletId: UserWalletId,
        response: UserTokensResponse,
        onFailSend: () -> Unit,
    ) {
        safeApiCall(
            call = {
                val accountsResponse = tangemTechApi.getWalletAccountsV2(walletId = userWalletId.stringValue)
                storeETag(userWalletId = userWalletId, apiResponse = accountsResponse)

                // The raw response carries no accountId on the tokens — it is implied by the owning account,
                // the same way DefaultWalletAccountsFetcher enriches it after every GET
                val fresh = accountsResponse.bind().toResponse()
                val remoteTokens = fresh.accounts.flatMap { account ->
                    account.tokens.orEmpty().map { token -> token.copy(accountId = account.id) }
                } + fresh.unassignedTokens

                // Token.equals compares the identity fields (network, derivation, contract), which is exactly
                // the dedup key here: our push stays authoritative, remote-only tokens are appended
                val pushedTokens = response.tokens.toSet()
                val merged = response.copy(
                    tokens = response.tokens + remoteTokens.filterNot { it in pushedTokens },
                )

                saveTokensWithETag(userWalletId = userWalletId, response = merged).bind()
            },
            onError = { error ->
                TangemLogger.e("Failed to merge and re-push user tokens after a 412", error)
                onFailSend()
            },
        )
    }

    private suspend fun getETagOrNull(userWalletId: UserWalletId): String? {
        if (!jointAccountFeatureToggles.isJointAccountEnabled) return null

        return walletAccountsETags.getSyncOrNull(userWalletId = userWalletId)
    }

    private suspend fun refreshETag(userWalletId: UserWalletId, apiResponse: ApiResponse<*>) {
        if (!jointAccountFeatureToggles.isJointAccountEnabled) return

        walletAccountsETags.refresh(userWalletId = userWalletId, apiResponse = apiResponse)
    }

    private suspend fun storeETag(userWalletId: UserWalletId, apiResponse: ApiResponse<*>) {
        if (!jointAccountFeatureToggles.isJointAccountEnabled) return

        walletAccountsETags.store(userWalletId = userWalletId, apiResponse = apiResponse)
    }

    /**
     * Drops fully identical tokens from the list.
     *
     * The API rejects the whole list with `400 All tokens's elements must be unique` if it contains two equal
     * elements, so a single duplicate discards the update of the entire wallet's token list. Duplicates are not
     * expected here, hence the error log.
     *
     * Tokens are compared by every field of the request instead of [UserTokensResponse.Token.equals], which
     * deliberately ignores most of them: dropping an element that differs in any way would silently change what the
     * user has saved.
     */
    private fun UserTokensResponse.withoutDuplicates(): UserTokensResponse {
        val uniqueTokens = tokens.distinctBy { it.toRequestKey() }

        if (uniqueTokens.size == tokens.size) return this

        TangemLogger.e("Dropped ${tokens.size - uniqueTokens.size} duplicated tokens before pushing them")

        return copy(tokens = uniqueTokens)
    }

    private fun UserTokensResponse.Token.toRequestKey(): List<Any?> {
        return listOf(
            id,
            accountId,
            networkId,
            derivationPath,
            name,
            symbol,
            decimals,
            contractAddress,
            addresses,
            dynamicAddressesEnabled,
        )
    }

    private suspend fun UserTokensResponse.enrichIf(
        userWalletId: UserWalletId,
        condition: Boolean,
    ): UserTokensResponse {
        if (!condition) return this

        return this
            .enrichByAddress(userWalletId = userWalletId)
            .enrichByAccountId(userWalletId = userWalletId)
    }

    private suspend fun UserTokensResponse.enrichByAddress(userWalletId: UserWalletId): UserTokensResponse {
        return addressesEnricher(userWalletId = userWalletId, response = this)
    }

    private fun UserTokensResponse.enrichByAccountId(userWalletId: UserWalletId): UserTokensResponse {
        return UserTokensResponseAccountIdEnricher(userWalletId = userWalletId, response = this)
    }

    private fun createPushTokensRetryer(userWalletId: UserWalletId, response: UserTokensResponse): Retryer {
        return Retryer(attempt = 3) { iteration ->
            var isSuccess = true

            push(
                userWalletId = userWalletId,
                response = response,
                onFailSend = {
                    TangemLogger.e(
                        "Retryer: Failed to push updated tokens on attempt ${iteration + 1} for $userWalletId",
                    )

                    isSuccess = false
                },
            )

            isSuccess
        }
    }
}