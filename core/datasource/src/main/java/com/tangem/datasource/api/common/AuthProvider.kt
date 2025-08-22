package com.tangem.datasource.api.common

/**
 * Provides auth for tangemTech API
 */
interface AuthProvider {

    /**
     * Returns authToken for tangem tech api
     */
    suspend fun getCardPublicKey(): String

    suspend fun getCardId(): String

    /**
     * Returns map where keys(cardId) associated with cardPublicKey
     */
    suspend fun getCardsPublicKeys(): Map<String, String>
}