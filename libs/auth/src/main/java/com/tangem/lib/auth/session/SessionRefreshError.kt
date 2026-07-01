package com.tangem.lib.auth.session

/**
 * Typed failure mode of `SessionTokenRefresher.refresh()`. Distinguishes terminal failures
 * (re-registration required) from transient ones (network / server) so callers can decide
 * whether to retry, surface UI, or trigger deferred-registration flow.
 */
sealed class SessionRefreshError {

    /** API-level error from `/refresh`, `/authenticate` or `/nonce/auth`. Transient unless [cause] says otherwise. */
    data class Api(val cause: AuthError) : SessionRefreshError()

    /**
     * Terminal — `/authenticate` returned 401/403. Session store was cleared; the device must
     * re-register ([REDACTED_TASK_KEY] / deferred-registration flow).
     */
    data object SessionRevoked : SessionRefreshError()

    /** Device key is not provisioned in Keystore (registration not yet run, or Keystore unavailable). */
    data object DeviceKeyUnavailable : SessionRefreshError()

    /** RSA/OAEP decryption of the server-issued auth nonce failed. */
    data class NonceDecryptionFailed(val cause: Throwable) : SessionRefreshError()

    /** Device-key signing of the authentication payload failed (Keystore I/O or ECDSA failure). */
    data class SigningFailed(val cause: Throwable) : SessionRefreshError()

    /** Refresher is disabled via `AND_15438_BACKEND_AUTHENTICATION_ENABLED` feature toggle. */
    data object Disabled : SessionRefreshError()
}