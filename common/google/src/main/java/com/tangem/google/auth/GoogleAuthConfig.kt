package com.tangem.google.auth

/**
 * Configuration for Google authentication.
 *
 * @property webClientId OAuth web client the Google ID token audience is issued for. A `null` or empty
 * value means no client is configured, so the sign-in flow cannot complete.
 */
data class GoogleAuthConfig(
    val webClientId: String?,
)