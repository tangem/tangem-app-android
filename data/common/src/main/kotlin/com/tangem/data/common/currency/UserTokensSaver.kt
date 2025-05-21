package com.tangem.data.common.currency

import com.tangem.data.common.api.safeApiCall
import com.tangem.data.common.tokens.UserTokensBackwardCompatibility
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.storeObject
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import timber.log.Timber

class UserTokensSaver constructor(
    private val tangemTechApi: TangemTechApi,
    private val appPreferencesStore: AppPreferencesStore,
    private val dispatchers: CoroutineDispatcherProvider,
    private val userTokensResponseAddressesEnricher: UserTokensResponseAddressesEnricher,
) {
    private val userTokensBackwardCompatibility = UserTokensBackwardCompatibility()

    suspend fun store(userWalletId: UserWalletId, response: UserTokensResponse, useEnricher: Boolean = true) =
        withContext(dispatchers.io) {
            val compatibleUserTokensResponse = userTokensBackwardCompatibility.applyCompatibilityAndGetUpdated(response)
            val enrichedUserTokensResponse = if (useEnricher) {
                userTokensResponseAddressesEnricher(
                    userWalletId = userWalletId,
                    response = compatibleUserTokensResponse,
                )
            } else {
                compatibleUserTokensResponse
            }
            appPreferencesStore.storeObject(
                key = PreferencesKeys.getUserTokensKey(userWalletId = userWalletId.stringValue),
                value = enrichedUserTokensResponse,
            )
        }

    suspend fun storeAndPush(userWalletId: UserWalletId, response: UserTokensResponse) {
        val enrichedUserTokensResponse = userTokensResponseAddressesEnricher(
            userWalletId = userWalletId,
            response = response,
        )
        store(userWalletId, enrichedUserTokensResponse, false)
        push(userWalletId, enrichedUserTokensResponse, false)
    }

    suspend fun push(userWalletId: UserWalletId, response: UserTokensResponse, useEnricher: Boolean = true) =
        withContext(dispatchers.io) {
            val enrichedUserTokensResponse = if (useEnricher) {
                userTokensResponseAddressesEnricher(
                    userWalletId = userWalletId,
                    response = response,
                )
            } else {
                response
            }
            safeApiCall({ tangemTechApi.saveUserTokens(userWalletId.stringValue, enrichedUserTokensResponse).bind() }) {
                Timber.e(it, "Unable to push user tokens for: ${userWalletId.stringValue}")
            }
        }
}