package com.tangem.lib.auth.session

/**
 * A server-issued, deciphered wallet nonce, ready to be signed. Produced by
 * [WalletRegistrar.requestNonce] (no card involved) and handed back to [WalletRegistrar.prepare].
 *
 * Single-use: the server consumes it on registration. It is device-scoped rather than tied to a
 * specific wallet id, so it can be prefetched before the wallet is known — but it still backs exactly
 * one registration and must not be reused across registrations.
 *
 * @property nonce the base64url string echoed back to the server in the registration request.
 * @property nonceBytes the decoded raw bytes the signature is produced over.
 */
class WalletNonce internal constructor(
    internal val nonce: String,
    internal val nonceBytes: ByteArray,
)