package com.tangem.data.cloudbackup.crypto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Cloud backup file structure — a small password-encrypted keystore wrapped in JSON.
 *
 * The payload is encrypted with [CloudBackupCipher] (AES-256-GCM + Argon2id). The [name], [walletId] and

 * render the backups list (wallet name + creation date) before decryption.
 *
 * @property version   backup file format version, independent of the Ethereum Keystore version
 * @property walletId  id of the backed up wallet, used to identify and de-duplicate backups

 */
@Serializable
internal data class CloudBackupFileData(
    @SerialName("version") val version: Int,
    @SerialName("id") val id: String,
    @SerialName("name") val name: String? = null,
    @SerialName("walletId") val walletId: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("crypto") val crypto: CryptoData,
) {

    /**
     * @property cipher     symmetric cipher name, only [CloudBackupCipher.CIPHER_AES_256_GCM] is supported
     * @property ciphertext hex-encoded ciphertext (without the authentication tag)
     * @property tag        hex-encoded GCM authentication tag (16 bytes)
     * @property kdf        key derivation function name, [CloudBackupCipher.KDF_ARGON2ID]
     */
    @Serializable
    data class CryptoData(
        @SerialName("cipher") val cipher: String,
        @SerialName("cipherparams") val cipherparams: CipherParams,
        @SerialName("ciphertext") val ciphertext: String,
        @SerialName("tag") val tag: String,
        @SerialName("kdf") val kdf: String,
        @SerialName("kdfparams") val kdfparams: KdfParams,
    )

    /** @property nonce hex-encoded AES-GCM nonce (12 bytes) */
    @Serializable
    data class CipherParams(
        @SerialName("nonce") val nonce: String,
    )

    /**
     * @property version     Argon2 version number (0x13)
     * @property memory      Argon2 memory cost in KiB
     * @property iterations  Argon2 time cost / passes
     * @property parallelism Argon2 lanes
     * @property dklen       derived key length in bytes
     * @property salt        hex-encoded KDF salt
     */
    @Serializable
    data class KdfParams(
        @SerialName("version") val version: Int,
        @SerialName("memory") val memory: Int,
        @SerialName("iterations") val iterations: Int,
        @SerialName("parallelism") val parallelism: Int,
        @SerialName("dklen") val dklen: Int,
        @SerialName("salt") val salt: String,
    )
}