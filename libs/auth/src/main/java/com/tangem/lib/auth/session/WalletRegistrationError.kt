package com.tangem.lib.auth.session

/**
 * Typed failure mode of `WalletRegistrar.register()`. Mirrors [DeviceRegistrationError] but covers
 * the wallet-binding paths (`/nonce/wallet` + `/wallet`).
 */
sealed class WalletRegistrationError {

    /** API-level error from `/nonce/wallet` or `/wallet`. Transient unless [cause] says otherwise. */
    data class Api(val cause: AuthError) : WalletRegistrationError()

    /** Device key is not provisioned in Keystore (registration cannot proceed without one). */
    data object DeviceKeyUnavailable : WalletRegistrationError()

    /** RSA/OAEP decryption of the server-issued wallet nonce failed. */
    data class NonceDecryptionFailed(val cause: Throwable) : WalletRegistrationError()

    /** Base64url-decoding the (decrypted) wallet nonce into its raw signing bytes failed. */
    data class NonceDecodingFailed(val cause: Throwable) : WalletRegistrationError()

    /**
     * Producing the wallet/card signature failed — Card SDK / hot SDK error, or the user cancelled
     * the NFC tap / biometric prompt.
     */
    data class SigningFailed(val cause: Throwable) : WalletRegistrationError()

    /**
     * Persisting the reissued tokens or the registered-wallet marker failed (DataStore I/O). The
     * marker stays unset, so the next attempt retries cleanly.
     */
    data class PersistenceFailed(val cause: Throwable) : WalletRegistrationError()

    /** Registrar is disabled via `AND_15438_BACKEND_AUTHENTICATION_ENABLED` feature toggle. */
    data object Disabled : WalletRegistrationError()

    /**
     * No session tokens are present, so `/wallet/register` (which is session-authenticated) would go
     * out unauthenticated and 401. Registration bails before consuming the one-time wallet nonce and
     * retries idempotently on a later attempt, once the device has an active session.
     */
    data object NoActiveSession : WalletRegistrationError()
}