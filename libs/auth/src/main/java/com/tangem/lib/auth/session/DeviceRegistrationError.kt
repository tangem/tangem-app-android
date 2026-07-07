package com.tangem.lib.auth.session

/**
 * Typed failure mode of `DeviceRegistrar.register()`. Mirrors [SessionRefreshError] but covers
 * the registration-specific paths (`/nonce/device` + `/register`).
 */
sealed class DeviceRegistrationError {

    /** API-level error from `/nonce/device` or `/register`. Transient unless [cause] says otherwise. */
    data class Api(val cause: AuthError) : DeviceRegistrationError()

    /** Device key is not provisioned in Keystore (registration cannot proceed without one). */
    data object DeviceKeyUnavailable : DeviceRegistrationError()

    /** RSA/OAEP decryption of the server-issued device-registration nonce failed. */
    data class NonceDecryptionFailed(val cause: Throwable) : DeviceRegistrationError()

    /** Device-key signing of the registration payload failed (Keystore I/O or ECDSA failure). */
    data class SigningFailed(val cause: Throwable) : DeviceRegistrationError()

    /**
     * Persisting the freshly minted tokens or the `IS_DEVICE_REGISTERED_KEY` flag failed
     * (DataStore I/O). The flag stays `false`, so the next launch retries cleanly.
     */
    data class PersistenceFailed(val cause: Throwable) : DeviceRegistrationError()

    /** Registrar is disabled via `AND_15438_BACKEND_AUTHENTICATION_ENABLED` feature toggle. */
    data object Disabled : DeviceRegistrationError()
}