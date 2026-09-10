package com.tangem.lib.auth.api.config

/**
 * Configuration for the Tangem Auth service.
 *
 * @property authServiceKey base64-encoded key used to decrypt authentication nonces. A `null` or empty
 * value disables nonce decryption.
 */
data class AuthServiceConfig(
    val authServiceKey: String?,
)