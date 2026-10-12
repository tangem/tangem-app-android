package com.tangem.data.sociallogin.crypto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `social-backup.json` structure. Unlike the password-based cloud backup there
 * is no `kdf`/`kdfparams`/`salt`: the recovery key is used directly as the AES-256 key.
 *
 * The header is readable without the recovery key, and [version] with [walletId] also form the AAD, so
 * an edited header fails decryption.
 *

 */
@Serializable
internal data class SocialBackupFileData(
    @SerialName("version") val version: Int,
    @SerialName("walletId") val walletId: String,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("crypto") val crypto: CryptoData,
) {

    /**
     * @property cipher     symmetric cipher name, only [SocialBackupCipher.CIPHER_AES_256_GCM] is supported
     * @property ciphertext hex-encoded ciphertext without the authentication tag
     * @property tag        hex-encoded GCM authentication tag (16 bytes)
     */
    @Serializable
    data class CryptoData(
        @SerialName("cipher") val cipher: String,
        @SerialName("cipherparams") val cipherparams: CipherParams,
        @SerialName("ciphertext") val ciphertext: String,
        @SerialName("tag") val tag: String,
    )

    /** @property nonce hex-encoded AES-GCM nonce (12 bytes) */
    @Serializable
    data class CipherParams(
        @SerialName("nonce") val nonce: String,
    )
}