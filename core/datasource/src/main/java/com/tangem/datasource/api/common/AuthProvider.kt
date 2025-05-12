package com.tangem.datasource.api.common

/**
 * Provides auth for tangemTech API
 */
interface AuthProvider {

    /**
     * Returns authToken for tangem tech api
     */
    fun getCardPublicKey(): String

    fun getCardId(): String

    /**
     * Returns map where keys(cardId) associated with cardPublicKey
     */
    fun getCardsPublicKeys(): Map<String, String>
}