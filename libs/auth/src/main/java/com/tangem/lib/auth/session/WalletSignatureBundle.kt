package com.tangem.lib.auth.session

/**
 * Raw signature material a caller produces for a single wallet over the deciphered wallet nonce.
 * Produced by a [WalletSigner] (NFC card attestation for COLD wallets, hot SDK signing for MOBILE).
 *
 * `WalletRegistrar` Base64-encodes these bytes before building the network request, so all values
 * here are raw bytes — the caller must not pre-encode them.
 *
 * @property walletSignature 65-byte secp256k1 RSV signature (v = recId + 27) over
 * `sha256(nonceBytes || walletSignatureSalt)`. The server recovers the wallet public key from it.
 * @property walletSignatureSalt salt mixed into the wallet-signature hash.
 * @property cardSignature 65-byte secp256k1 RSV signature over
 * `sha256(walletPublicKey || nonceBytes || cardSignatureSalt || walletStatus)`. COLD wallets only;
 * `null` for MOBILE (hot) wallets.
 * @property cardSignatureSalt salt mixed into the card-signature hash. COLD wallets only.
 * @property walletStatusByte single byte describing wallet provenance on the card
 * (`0x82` = generated on card, `0xC2` = SEED imported). COLD wallets only.
 */
data class WalletSignatureBundle(
    val walletSignature: ByteArray,
    val walletSignatureSalt: ByteArray,
    val cardSignature: ByteArray?,
    val cardSignatureSalt: ByteArray?,
    val walletStatusByte: Byte?,
)

/**
 * Produces a [WalletSignatureBundle] for the wallet being registered, given the deciphered wallet
 * nonce bytes. Implemented in the app/data layer where the Card SDK / hot wallet SDK live; the
 * `WalletRegistrar` stays free of SDK dependencies.
 */
fun interface WalletSigner {
    suspend fun sign(nonceBytes: ByteArray): WalletSignatureBundle
}