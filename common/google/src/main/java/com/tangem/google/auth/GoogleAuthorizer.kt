package com.tangem.google.auth

import arrow.core.Either

/**
 * Reusable Google authorization core built on the Google Identity Services `AuthorizationClient`.
 *
 * Requests the given OAuth [scopes] and yields an access token. When the client needs user
 * interaction (account picker / consent) it returns a resolution that is launched through
 * [GoogleAuthActivityResultBridge]. The access token is never logged.
 *
 * The contract is scope-agnostic on purpose so that different features reuse the same authorization
 * flow, each passing its own scopes and mapping [GoogleAuthError] to its own domain errors.
 */
interface GoogleAuthorizer {

    suspend fun authorize(scopes: List<String>, interactive: Boolean = true): Either<GoogleAuthError, GoogleAuthResult>

    fun clearAuthorization()

    suspend fun clearToken(token: String)
}

data class GoogleAuthResult(
    val accessToken: String,
)

sealed interface GoogleAuthError {

    /** User cancelled the authorization flow */
    data object AuthCanceled : GoogleAuthError

    /** A token was requested silently (no user interaction), but interactive authorization is required */
    data object AuthRequired : GoogleAuthError

    /** Authorization finished, but the requested permissions were not granted */
    data object PermissionsMissing : GoogleAuthError

    /** Google authorization is not available on the device (no/outdated Google services) */
    data object Unavailable : GoogleAuthError

    /** Could not complete authorization because of connectivity issues */
    data object NetworkError : GoogleAuthError

    data class Unknown(val cause: Throwable? = null) : GoogleAuthError
}