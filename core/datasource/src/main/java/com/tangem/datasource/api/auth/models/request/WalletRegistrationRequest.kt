package com.tangem.datasource.api.auth.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Wallet registration request — binds a new wallet to an already-registered device.
 *
 * When [cardSignature] (and the accompanying [cardSignatureSalt] / [walletStatus]) is provided the
 * wallet is bound as COLD (card-backed); otherwise it is registered as a MOBILE (hot) wallet.
 * Mirrors the `WalletRegistrationRequest` schema in the backend OpenAPI contract.
 */
@JsonClass(generateAdapter = true)
data class WalletRegistrationRequest(
    /** Deciphered nonce value from `/api/v1/auth/nonce/wallet`. */
    @Json(name = "nonce") val nonce: String,
    /**
     * Wallet identifier — Base64-encoded
     * `HMAC-SHA256(key = SHA-256(walletPublicKey), data = "UserWalletID")`.
     */
    @Json(name = "walletId") val walletId: String,
    /**
     * Base64-encoded secp256k1 RSV signature (65 bytes) over `sha256(nonce || walletSignatureSalt)`.
     * The server recovers `walletPublicKey` from this signature.
     */
    @Json(name = "walletSignature") val walletSignature: String,
    /** Base64-encoded salt used in the wallet signature hash. */
    @Json(name = "walletSignatureSalt") val walletSignatureSalt: String,
    /**
     * Base64-encoded secp256k1 RSV signature (65 bytes) over
     * `sha256(walletPublicKey || nonce || cardSignatureSalt || walletStatus)`. Required for
     * cold-wallet registration; `null` for mobile (hot) wallets.
     */
    @Json(name = "cardSignature") val cardSignature: String?,
    /** Base64-encoded salt used in the card signature hash. Required for cold-wallet registration. */
    @Json(name = "cardSignatureSalt") val cardSignatureSalt: String?,
    /**
     * Base64-encoded single byte describing wallet provenance on the card
     * (`0x82` = generated on card, `0xC2` = SEED imported). Required for cold-wallet registration.
     */
    @Json(name = "walletStatus") val walletStatus: String?,
    /** Platform attestation token (Play Integrity / App Attest). */
    @Json(name = "attestationToken") val attestationToken: String?,
    /** Client-reported device metadata. */
    @Json(name = "metadata") val metadata: DeviceMetadata,
)