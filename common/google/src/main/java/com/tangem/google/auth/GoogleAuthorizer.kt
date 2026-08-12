package com.tangem.google.auth

import arrow.core.Either

/**
 * Reusable Google authorization core built on the Google Identity Services `AuthorizationClient`.
 *
 * Different features reuse the same flow, each passing its own [GoogleAuthScope]s and mapping
 * [GoogleAuthError] to its own domain errors. The concrete implementation launches the consent
 * resolution through an Activity-result launcher registered at the app root.
 */
interface GoogleAuthorizer {

    /**
     * Requests authorization for the given OAuth [scopes] and returns an access token.
     *
     * @param scopes OAuth scopes to request (e.g. Drive `drive.file`).
     * @param interactive when `true`, asks which account to use (unless one was already picked since the
     * last [clearAuthorization]) and shows the consent screen if the platform needs it; when `false`,
     * resolves only from an already-granted session, shows no UI and yields
     * [GoogleAuthError.AuthRequired] when interaction would be required.
     * @return the granted [GoogleAuthResult], or a [GoogleAuthError] describing the failure.
     */
    suspend fun authorize(
        scopes: List<GoogleAuthScope>,
        interactive: Boolean = true,
    ): Either<GoogleAuthError, GoogleAuthResult>

    /**
     * Forgets whatever authorization state the implementation itself holds — including the picked
     * account, so the next interactive [authorize] asks for it again and the user can switch accounts.
     * The Google Identity `AuthorizationClient` exposes no way to revoke a granted authorization, so
     * dropping the access token is up to the caller — see [clearToken].
     */
    fun clearAuthorization()

    /** Invalidates the given access [token] in the platform token cache so it cannot be reused. */
    suspend fun clearToken(token: String)
}

/** A single OAuth scope, e.g. `GoogleAuthScope("https://www.googleapis.com/auth/drive.file")`. */
@JvmInline
value class GoogleAuthScope(val value: String)

/** Successful authorization result. */
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