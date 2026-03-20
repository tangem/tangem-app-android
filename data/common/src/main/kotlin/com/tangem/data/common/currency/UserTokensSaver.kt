package com.tangem.data.common.currency

import com.tangem.data.common.api.safeApiCall
import com.tangem.data.common.wallet.WalletServerBinder
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.isNetworkError
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncOrNull
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
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

        val enrichedResponse = response.enrichIf(userWalletId = userWalletId, condition = useEnricher)

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
                val apiResponse = tangemTechApi.saveTokens(
                    userId = userWallet.walletId.stringValue,
                    userTokens = response,
                )

                val isWalletNotFound = apiResponse is ApiResponse.Error &&
                    apiResponse.cause.isNetworkError(ApiResponseError.HttpException.Code.NOT_FOUND)

                if (isWalletNotFound) {
                    walletServerBinder.bind(userWallet).bind()

                    tangemTechApi.saveTokens(
                        userId = userWallet.walletId.stringValue,
                        userTokens = response,
                    ).bind()
                } else {
                    apiResponse.bind()
                }
            },
            onError = { onFailSend() },
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