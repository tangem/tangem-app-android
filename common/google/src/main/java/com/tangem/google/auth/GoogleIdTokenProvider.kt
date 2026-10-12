package com.tangem.google.auth

import arrow.core.Either

/**
 * Sign-in with Google that yields an OIDC id token, used to prove the provider identity to a backend.
 *
 * Separate from [GoogleAuthorizer]: that one grants OAuth scopes for Google APIs (e.g. Drive), this one
 * only identifies the user. A feature may need both — they are independent flows with independent UI.
 */
interface GoogleIdTokenProvider {

    /** Whether sign-in with Google can run on this device at all, so the caller can hide the option */
    suspend fun isAvailable(): Boolean

    /**
     * Asks the user to pick a Google account and returns the id token issued for it.
     *
     * @param nonce server-issued value the token is bound to, so a token captured elsewhere cannot be
     * replayed against the backend. `null` leaves the token unbound — pass it deliberately.
     */
    suspend fun getIdToken(nonce: String?): Either<GoogleIdTokenError, GoogleIdToken>

    /** Drops the credential state the providers keep — auto sign-in and the silently reused account */
    suspend fun signOut()
}

/**
 * A completed sign-in.
 *
 * @property value the OIDC id token — a short-lived credential that must not reach logs or analytics.
 * @property accountName the account the token was issued for, in the form [GoogleAuthorizer.selectAccount]
 * expects — a scope grant that follows then has no reason to ask for it again.
 */
data class GoogleIdToken(val value: String, val accountName: String) {

    override fun toString(): String = "GoogleIdToken(***)"
}

sealed interface GoogleIdTokenError {

    data object Canceled : GoogleIdTokenError

    /** No Google account on the device, or none was granted */
    data object NoCredential : GoogleIdTokenError

    /** No Google services, no web client id, or no credential provider on the device */
    data object Unavailable : GoogleIdTokenError

    /**
     * Everything else, including the transient failures Credential Manager reports without saying what
     * went wrong — retryable, but not necessarily connectivity, so they are not narrowed here.
     */
    data class Unknown(val cause: Throwable? = null) : GoogleIdTokenError
}