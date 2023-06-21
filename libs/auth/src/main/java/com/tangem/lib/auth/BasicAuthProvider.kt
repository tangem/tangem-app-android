package com.tangem.lib.auth

/**
 * Provides auth for Tangem WebView
 */
interface BasicAuthProvider {

    fun getCredentials(): Map<String, String>
}