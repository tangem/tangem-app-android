package com.tangem.lib.auth.session

import arrow.core.Either

/**
 * Registers the device with the Tangem Auth Service and persists the initial session tokens.
 *
 * Idempotent and safe to call on every app launch:
 *  - on first run, fetches a ciphered nonce from `POST /api/v1/auth/nonce/device`, decrypts it
 *    with the app's RSA private key, signs a `RegisterPayload` with the device key, posts it to
 *    `POST /api/v1/auth/register`, persists the resulting `SessionTokens` and flips the
 *    "device registered" flag in `AppPreferencesStore`,
 *  - on subsequent runs, sees the flag and short-circuits without any network traffic.
 *
 * Tokens returned by `/register` are not surfaced to callers — they're written to
 * `SessionTokensStore` and accessed from there. The result type carries only success/failure
 * so callers can log/report transient errors.
 *
 * Implementations serialise concurrent callers so the server-issued nonce isn't consumed twice.
 */
interface DeviceRegistrar {

    suspend fun register(): Either<DeviceRegistrationError, Unit>
}