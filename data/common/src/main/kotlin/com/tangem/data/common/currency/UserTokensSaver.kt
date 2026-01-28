package com.tangem.data.common.currency

import com.tangem.data.common.api.safeApiCall
import com.tangem.data.common.tokens.UserTokensBackwardCompatibility
import com.tangem.data.common.wallet.WalletServerBinder
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.isNetworkError
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.WalletType
import com.tangem.datasource.local.appsflyer.AppsFlyerStore
import com.tangem.datasource.local.token.UserTokensResponseStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.account.featuretoggle.AccountsFeatureToggles
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.retryer.Retryer
import com.tangem.utils.retryer.RetryerPool
import kotlinx.coroutines.withContext
import timber.log.Timber

@Suppress("LongParameterList")
class UserTokensSaver(
    private val tangemTechApi: TangemTechApi,
    private val userWalletsStore: UserWalletsStore,
    private val userTokensResponseStore: UserTokensResponseStore,
    private val dispatchers: CoroutineDispatcherProvider,
    private val addressesEnricher: UserTokensResponseAddressesEnricher,
    private val walletServerBinder: WalletServerBinder,
    private val appsFlyerStore: AppsFlyerStore,
    private val accountsFeatureToggles: AccountsFeatureToggles,
    private val pushTokensRetryerPool: RetryerPool,
) {
    private val userTokensBackwardCompatibility = UserTokensBackwardCompatibility()

    suspend fun storeAndPush(userWalletId: UserWalletId, response: UserTokensResponse) {
        withContext(dispatchers.default) {
            val enrichedResponse = response.enrichIf(userWalletId = userWalletId, condition = true)

            store(userWalletId = userWalletId, response = enrichedResponse, useEnricher = false)
            push(userWalletId = userWalletId, response = enrichedResponse, useEnricher = false)
        }
    }

    suspend fun store(userWalletId: UserWalletId, response: UserTokensResponse, useEnricher: Boolean = true) =
        withContext(dispatchers.default) {
            val updatedResponse = response
                .applyCompatibility()
                .enrichIf(userWalletId = userWalletId, condition = useEnricher)

            userTokensResponseStore.store(userWalletId = userWalletId, response = updatedResponse)
        }

    suspend fun push(
        userWalletId: UserWalletId,
        response: UserTokensResponse,
        useEnricher: Boolean = true,
        onFailSend: () -> Unit = {},
    ) = withContext(dispatchers.io) {
        val userWallet = userWalletsStore.getSyncOrNull(key = userWalletId)

        if (userWallet == null) {
            Timber.e("UserWallet with id $userWalletId not found. Cannot push tokens.")
            onFailSend()
            return@withContext
        }

        if (accountsFeatureToggles.isFeatureEnabled) {
            val enrichedResponse = response.enrichIf(userWalletId = userWalletId, condition = useEnricher)

            pushNew(userWallet = userWallet, response = enrichedResponse, onFailSend = onFailSend)
        } else {
            val conversionData = appsFlyerStore.get()

            val enrichedResponse = response.enrichIf(userWalletId = userWalletId, condition = useEnricher).copy(
                walletName = userWallet.name.takeIf { it.isNotBlank() },
                walletType = WalletType.from(userWallet),
                refcode = conversionData?.refcode,
                campaign = conversionData?.campaign,
            )

            pushLegacy(userWalletId = userWalletId, response = enrichedResponse, onFailSend = onFailSend)
        }
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

    private suspend fun pushLegacy(userWalletId: UserWalletId, response: UserTokensResponse, onFailSend: () -> Unit) {
        safeApiCall(
            call = { tangemTechApi.saveUserTokens(userId = userWalletId.stringValue, userTokens = response).bind() },
            onError = { onFailSend() },
        )
    }

    private suspend fun pushNew(userWallet: UserWallet, response: UserTokensResponse, onFailSend: () -> Unit) {
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

    private fun UserTokensResponse.applyCompatibility(): UserTokensResponse {
        return userTokensBackwardCompatibility.applyCompatibilityAndGetUpdated(userTokensResponse = this)
    }

    private suspend fun UserTokensResponse.enrichIf(
        userWalletId: UserWalletId,
        condition: Boolean,
    ): UserTokensResponse {
        if (!condition) return this

        return this
            .enrichByAddress(userWalletId = userWalletId)
            .let { response ->
                if (accountsFeatureToggles.isFeatureEnabled) {
                    response.enrichByAccountId(userWalletId = userWalletId)
                } else {
                    response
                }
            }
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
                    Timber.e(
                        "Retryer: Failed to push updated tokens on attempt ${iteration + 1} for $userWalletId",
                    )

                    isSuccess = false
                },
            )

            isSuccess
        }
    }
}