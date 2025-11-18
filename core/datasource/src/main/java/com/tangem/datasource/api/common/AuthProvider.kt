package com.tangem.datasource.api.common

import com.tangem.datasource.api.common.config.ApiEnvironment
import com.tangem.utils.Provider
import com.tangem.utils.ProviderSuspend

/**
 * Provides auth for tangemTech API
 */
interface AuthProvider {

    /**
     * Returns authToken for tangem tech api
     */
    suspend fun getCardPublicKey(): String

    suspend fun getCardId(): String

    fun getApiKey(apiEnvironment: Provider<ApiEnvironment>): ProviderSuspend<String>

    /**
     * Returns map where keys(cardId) associated with cardPublicKey
     */
    suspend fun getCardsPublicKeys(): Map<String, String>
}