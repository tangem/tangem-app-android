package com.tangem.lib.auth.session

import arrow.core.Either

/**
 * Binds a wallet to the already-registered device with the Tangem Auth Service, proving wallet
 * (and, for cold cards, card) ownership over a server-issued wallet nonce.
 *
 * Idempotent per wallet: once a `walletId` is registered it is remembered, and subsequent calls
 * for it short-circuit without network traffic. Layered on top of device registration — requires a
 * valid DPoP-bound session (see `DeviceRegistrar` / `SessionTokenRefresher`).
 *
 * Signing is delegated to the caller via [WalletSigner]: the registrar owns the nonce request,
 * decryption, request assembly, POST and persistence, but the NFC/biometric signature itself is
 * produced in the app layer (the registrar has no Card/hot SDK dependency). The nonce must be
 * fetched before signing (the signature is over the nonce), so the registrar fetches it and hands
 * the deciphered bytes to the signer.
 *
 * The nonce request is card-independent, so COLD callers can prefetch it via [requestNonce] before
 * the card is tapped (overlapping the round-trip with the tap) and feed the result into [prepare].
 *
 * Tokens returned by wallet registration are written to `SessionTokensStore`, not surfaced to callers — the
 * result type carries only success/failure so callers can log transient errors.
 */
interface WalletRegistrar {

    /**
     * Registers the wallet identified by [walletId] (Base64 `UserWalletId`) in one shot: nonce
     * request, signing and POST. Use this when there is no session/UX constraint (MOBILE wallets).
     */
    suspend fun register(walletId: String, signer: WalletSigner): Either<WalletRegistrationError, Unit>

    /**
     * Best-effort check of whether [walletId] (Base64 `UserWalletId`) is already registered. Lets a
     * COLD caller skip the nonce request entirely for a known, already-registered card (e.g. every
     * subsequent scan). Returns `false` if the marker store can't be read — the authoritative
     * idempotency check still runs inside [prepare], so a stale `false` only costs one nonce request.
     */
    suspend fun isRegistered(walletId: String): Boolean

    /**
     * Whether a device session exists, i.e. a registration attempt can reach the backend at all. Lets a
     * caller skip a user-facing step (an unlock prompt) that would be wasted on a registration bound to
     * fail with [WalletRegistrationError.NoActiveSession]. Returns `false` if the token store can't be read.
     */
    suspend fun hasActiveSession(): Boolean

    /**
     * Phase 0 (COLD prefetch): requests and deciphers a wallet nonce. Card-independent — needs only
     * a valid session and the device key — so it can run before the card is tapped, letting the
     * network round-trip overlap the tap. Feed the result into [prepare].
     */
    suspend fun requestNonce(): Either<WalletRegistrationError, WalletNonce>

    /**
     * Phase 1: idempotency check and signing via [signer] over the prefetched [nonce]. For COLD
     * wallets this must run while the card session is open (the signer taps the card). Returns the
     * assembled registration to hand to [submit] later, or `null` if the wallet is already
     * registered (no-op). Performs NO network write — pair it with [submit] after the session closes.
     *
     * [shouldForce] skips the idempotency check, for a wallet whose local marker no longer reflects its
     * server-side binding (e.g. a mobile wallet upgraded to a card keeps the same walletId).
     */
    suspend fun prepare(
        walletId: String,
        nonce: WalletNonce,
        signer: WalletSigner,
        shouldForce: Boolean = false,
    ): Either<WalletRegistrationError, PreparedWalletRegistration?>

    /**
     * Drops the local registration marker of [walletId] (no network), so the next registration attempt
     * is no longer skipped. The server-side binding is left as is.
     */
    suspend fun forgetRegistration(walletId: String): Either<WalletRegistrationError, Unit>

    /**
     * Phase 2: sends the [prepared] registration, persists the reissued tokens, and marks the wallet
     * registered. No card needed — safe to run after the session has closed.
     */
    suspend fun submit(prepared: PreparedWalletRegistration): Either<WalletRegistrationError, Unit>

    /**
     * Unbinds a previously registered wallet from the authenticated device. Persists the fresh
     * session tokens returned by the server and drops the local registration marker.
     */
    suspend fun unregister(walletId: String): Either<WalletRegistrationError, Unit>

    /**
     * Drops ALL local registration markers (no network). Called when the device is re-registered: the
     * old device record — and every wallet binding on it — is gone server-side, so the markers are
     * stale and would otherwise make [isRegistered]/[prepare] skip wallets that must now re-register.
     */
    suspend fun clearRegistrations()
}