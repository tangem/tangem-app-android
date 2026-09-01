package com.tangem.lib.auth.session

import arrow.core.Either

/**
 * Registers the device with the Tangem Auth Service and persists the initial session tokens.
 *
 * Idempotent and safe to call on every app launch:
 *  - on first run, fetches a ciphered nonce, decrypts it with the app's RSA private key, signs a
 *    `RegisterPayload` with the device key, registers the device, persists the resulting
 *    `SessionTokens` and flips the "device registered" flag in `AppPreferencesStore`,
 *  - on subsequent runs, sees the flag and short-circuits without any network traffic.
 *
 * Tokens returned by registration are not surfaced to callers — they're written to
 * `SessionTokensStore` and accessed from there. The result type carries only success/failure
 * so callers can log/report transient errors.
 *
 * Implementations serialise concurrent callers so the server-issued nonce isn't consumed twice.
 */
interface DeviceRegistrar {

    suspend fun register(): Either<DeviceRegistrationError, Unit>

    /**
     * Forces a fresh device registration, ignoring the local "device registered" flag: resets the
     * flag and re-runs the full registration (nonce + register). Recovery path for when the backend
     * reports the device is unknown ("Device not found" on `/authenticate`) while the local flag is
     * still set — e.g. after backend-side data drift or record loss. The flag is cleared up front, so
     * even a failed attempt self-heals on the next launch (when [register] runs again).
     */
    suspend fun reregister(): Either<DeviceRegistrationError, Unit>
}