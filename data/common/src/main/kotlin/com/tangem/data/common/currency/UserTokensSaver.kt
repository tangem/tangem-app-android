package com.tangem.data.common.currency

import com.tangem.data.common.api.safeApiCall
import com.tangem.data.common.cache.etag.ETagsStore
import com.tangem.data.common.wallet.WalletServerBinder
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ETAG_HEADER
import com.tangem.datasource.api.common.response.isNetworkError
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.account.flattenTokens
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
    private val eTagsStore: ETagsStore,
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
                val apiResponse = saveTokensWithETag(userWalletId = userWallet.walletId, response = response)

                val isWalletNotFound = apiResponse is ApiResponse.Error &&
                    apiResponse.cause.isNetworkError(ApiResponseError.HttpException.Code.NOT_FOUND)

                if (isWalletNotFound) {
                    walletServerBinder.bind(userWallet).bind()

                    saveTokensWithETag(userWalletId = userWallet.walletId, response = response).bind()
                } else {
                    apiResponse.bind()
                }
            },
            onError = { error ->
                if (error.isNetworkError(ApiResponseError.HttpException.Code.PRECONDITION_FAILED)) {
                    mergeAndRetry(userWalletId = userWallet.walletId, response = response, onFailSend = onFailSend)
                } else {
                    TangemLogger.e("Failed to push ${response.tokens.size} user tokens", error)
                    onFailSend()
                }
            },
        )
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
            updateETag(userWalletId = userWalletId, apiResponse = apiResponse)
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
                if (accountsResponse is ApiResponse.Success) {
                    updateETag(userWalletId = userWalletId, apiResponse = accountsResponse)
                }

                val fresh = accountsResponse.bind().toResponse()
                val remoteTokens = fresh.flattenTokens() + fresh.unassignedTokens

                val pushedKeys = response.tokens.map { it.toIdentityKey() }.toSet()
                val merged = response.copy(
                    tokens = response.tokens + remoteTokens.filterNot { it.toIdentityKey() in pushedKeys },
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

        return eTagsStore.getSyncOrNull(userWalletId = userWalletId, key = ETagsStore.Key.WalletAccounts)
    }

    private suspend fun updateETag(userWalletId: UserWalletId, apiResponse: ApiResponse<*>) {
        if (!jointAccountFeatureToggles.isJointAccountEnabled) return

        val eTag = apiResponse.headers[ETAG_HEADER]?.firstOrNull()

        if (eTag != null) {
            eTagsStore.store(userWalletId = userWalletId, key = ETagsStore.Key.WalletAccounts, value = eTag)
        } else {
            eTagsStore.clear(userWalletId = userWalletId, key = ETagsStore.Key.WalletAccounts)
        }
    }

    private fun UserTokensResponse.Token.toIdentityKey(): List<Any?> {
        return listOf(id, accountId, networkId, derivationPath, contractAddress)
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