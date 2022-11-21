package com.tangem.lib.auth

/**
 * Provides auth for tangemTech API
 */
interface AuthProvider {

    /**
     * Returns authToken for tangem tech api
     */
    fun getAuthToken(): String
}