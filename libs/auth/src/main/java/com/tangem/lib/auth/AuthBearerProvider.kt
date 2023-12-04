package com.tangem.lib.auth

/**
 * Provides auth for tangemTech API
 */
interface AuthBearerProvider {

    /**
     * Returns api-key
     */
    fun getApiKey(): String
}