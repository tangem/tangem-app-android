package com.tangem.lib.auth

/**
 * Provides auth for tangemTech API
 */
interface LazyAuthProvider {

    /**
     * Returns authToken for tangem tech api
     */
    fun getCardPublicKeyProvider(): () -> String

    fun getCardIdProvider(): () -> String
}