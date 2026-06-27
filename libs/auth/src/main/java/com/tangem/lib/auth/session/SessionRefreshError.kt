package com.tangem.lib.auth.session

/**
 * Typed failure mode of `SessionTokenRefresher.refresh()`. Distinguishes terminal failures
 * (re-registration required, server-side block) from transient ones (network / server) so callers
 * can decide whether to retry, surface UI, or trigger deferred-registration flow.
 */
sealed class SessionRefreshError {

    /** API-level error from `/refresh`, `/authenticate` or `/nonce/auth`. Transient unless [cause] says otherwise. */
    data class Api(val cause: AuthError) : SessionRefreshError()

    /**
     * Terminal — `/authenticate` returned 401/403. Session store was cleared; the device must
     * re-register.
     */
    data object SessionRevoked : SessionRefreshError()

    /**
     * Terminal — `/refresh` returned 403 (RED tier). The device is server-side blocked;
     * `/authenticate` won't help (it would also return 403). The client should not retry within
     * the current session — only attempt `/refresh` again on the next app launch, in case the
     * server-side block was lifted.
     */
    data object DeviceBlocked : SessionRefreshError()

    /** Device key is not provisioned in Keystore (registration not yet run, or Keystore unavailable). */
    data object DeviceKeyUnavailable : SessionRefreshError()

    /** RSA/OAEP decryption of the server-issued auth nonce failed. */
    data class NonceDecryptionFailed(val cause: Throwable) : SessionRefreshError()

    /** Device-key signing of the authentication payload failed (Keystore I/O or ECDSA failure). */
    data class SigningFailed(val cause: Throwable) : SessionRefreshError()

    /** Refresher is disabled via `AND_15438_BACKEND_AUTHENTICATION_ENABLED` feature toggle. */
    data object Disabled : SessionRefreshError()
}