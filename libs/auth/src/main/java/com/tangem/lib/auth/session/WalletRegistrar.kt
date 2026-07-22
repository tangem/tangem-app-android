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
 * Tokens returned by wallet registration are written to `SessionTokensStore`, not surfaced to callers — the
 * result type carries only success/failure so callers can log transient errors.
 */
interface WalletRegistrar {

    /**
     * Registers the wallet identified by [walletId] (Base64 `UserWalletId`) in one shot: [prepare]
     * followed by [submit]. Use this when there is no session/UX constraint (MOBILE wallets).
     */
    suspend fun register(walletId: String, signer: WalletSigner): Either<WalletRegistrationError, Unit>

    /**
     * Phase 1: idempotency check, nonce request/decryption, and signing via [signer]. For COLD
     * wallets this must run while the card session is open (the signer taps the card). Returns the
     * assembled registration to hand to [submit] later, or `null` if the wallet is already
     * registered (no-op). Performs NO network write — pair it with [submit] after the session closes.
     */
    suspend fun prepare(
        walletId: String,
        signer: WalletSigner,
    ): Either<WalletRegistrationError, PreparedWalletRegistration?>

    /**
     * Phase 2: sends the [prepared] registration, persists the reissued tokens, and marks the wallet
     * registered. No card needed — safe to run after the session has closed.
     */
    suspend fun submit(prepared: PreparedWalletRegistration): Either<WalletRegistrationError, Unit>
}