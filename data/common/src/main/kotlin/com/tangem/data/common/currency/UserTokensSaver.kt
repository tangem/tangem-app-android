package com.tangem.data.common.currency

import com.tangem.data.common.api.safeApiCall
import com.tangem.data.common.tokens.UserTokensBackwardCompatibility
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.local.token.UserTokensResponseStore
import com.tangem.domain.account.featuretoggle.AccountsFeatureToggles
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

class UserTokensSaver(
    private val tangemTechApi: TangemTechApi,
    private val userTokensResponseStore: UserTokensResponseStore,
    private val dispatchers: CoroutineDispatcherProvider,
    private val addressesEnricher: UserTokensResponseAddressesEnricher,
    private val accountsFeatureToggles: AccountsFeatureToggles,
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
    ) {
        withContext(dispatchers.default) {
            val enrichedResponse = response.enrichIf(userWalletId = userWalletId, condition = useEnricher)

            safeApiCall(
                call = {
                    withContext(dispatchers.io) {
                        tangemTechApi.saveUserTokens(userId = userWalletId.stringValue, userTokens = enrichedResponse)
                            .bind()
                    }
                },
                onError = { onFailSend() },
            )
        }
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
            .let {
                if (accountsFeatureToggles.isFeatureEnabled) {
                    it.enrichByAccountId(userWalletId = userWalletId)
                } else {
                    it
                }
            }
    }

    private suspend fun UserTokensResponse.enrichByAddress(userWalletId: UserWalletId): UserTokensResponse {
        return addressesEnricher(userWalletId = userWalletId, response = this)
    }

    private fun UserTokensResponse.enrichByAccountId(userWalletId: UserWalletId): UserTokensResponse {
        return UserTokensResponseAccountIdEnricher(userWalletId = userWalletId, response = this)
    }
}