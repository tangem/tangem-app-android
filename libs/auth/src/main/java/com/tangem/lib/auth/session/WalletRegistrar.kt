package com.tangem.lib.auth.session

import arrow.core.Either

/**
 * Binds a wallet to the already-registered device with the Tangem Auth Service
 * (`POST /api/v1/auth/wallet`), proving wallet (and, for cold cards, card) ownership over a
 * server-issued wallet nonce.
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
 * Tokens returned by `/wallet` are written to `SessionTokensStore`, not surfaced to callers — the
 * result type carries only success/failure so callers can log transient errors.
 */
interface WalletRegistrar {

    /**
     * Registers the wallet identified by [walletId] (Base64 `UserWalletId`), using [signer] to
     * produce the signature material over the deciphered wallet nonce.
     */
    suspend fun register(walletId: String, signer: WalletSigner): Either<WalletRegistrationError, Unit>
}