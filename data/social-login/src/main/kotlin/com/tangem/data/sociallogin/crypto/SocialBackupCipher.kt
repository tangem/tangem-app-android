package com.tangem.data.sociallogin.crypto

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.tangem.domain.sociallogin.models.RecoveryKey
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * The AAD is rebuilt from the file's own `version` and `walletId` rather than stored, so an edited
 * header — or a payload lifted out of another backup — fails the tag check. A wrong key and a tampered
 * file are the same outcome by design.
 */
internal class SocialBackupCipher(
    private val random: SecureRandom = SecureRandom(),
) {

    /** @param createdAt ISO 8601 UTC */
    fun encrypt(
        payload: ByteArray,
        recoveryKey: RecoveryKey,
        walletId: String,
        createdAt: String,
    ): SocialBackupFileData {
        val nonce = ByteArray(GCM_NONCE_SIZE_BYTES).also(random::nextBytes)

        // AES-GCM appends the tag to the ciphertext; the format keeps the two apart
        val sealed = aesGcm(
            mode = Cipher.ENCRYPT_MODE,
            key = recoveryKey.value,
            nonce = nonce,
            input = payload,
            aad = aad(version = VERSION, walletId = walletId),
        )

        return SocialBackupFileData(
            version = VERSION,
            walletId = walletId,
            createdAt = createdAt,
            crypto = SocialBackupFileData.CryptoData(
                cipher = CIPHER_AES_256_GCM,
                cipherparams = SocialBackupFileData.CipherParams(nonce = nonce.toHex()),
                ciphertext = sealed.copyOfRange(0, sealed.size - GCM_TAG_SIZE_BYTES).toHex(),
                tag = sealed.copyOfRange(sealed.size - GCM_TAG_SIZE_BYTES, sealed.size).toHex(),
            ),
        )
    }

    fun decrypt(data: SocialBackupFileData, recoveryKey: RecoveryKey): Either<SocialBackupCryptoError, ByteArray> =
        either {
            val decoded = validate(data)

            try {
                aesGcm(
                    mode = Cipher.DECRYPT_MODE,
                    key = recoveryKey.value,
                    nonce = decoded.nonce,
                    // Java's AES-GCM expects the tag appended, so re-join them
                    input = decoded.ciphertext + decoded.tag,
                    aad = aad(version = data.version, walletId = data.walletId),
                )
            } catch (e: AEADBadTagException) {
                raise(SocialBackupCryptoError.InvalidFormat("Authentication failed"))
            } catch (e: GeneralSecurityException) {
                raise(SocialBackupCryptoError.InvalidFormat("Decryption failed: ${e.message}"))
            }
        }

    fun validateFormat(data: SocialBackupFileData): Either<SocialBackupCryptoError, Unit> =
        either { validate(data) }.map { }

    /**
     * The part of the format a caller can rely on without the recovery key. Deliberately says nothing
     * about the version: telling whose backup the folder holds, and replacing or deleting it, has to
     * keep working for a file a newer app wrote.
     */
    fun validateHeader(data: SocialBackupFileData): Either<SocialBackupCryptoError, Unit> =
        either { ensureReadableWalletId(data) }

    private fun Raise<SocialBackupCryptoError>.validate(data: SocialBackupFileData): DecodedCryptoParams {
        // the version decides the parameters, so it is checked before anything is decoded
        ensure(data.version == VERSION) { SocialBackupCryptoError.UnsupportedVersion(data.version) }
        ensureReadableWalletId(data)
        ensure(data.crypto.cipher == CIPHER_AES_256_GCM) {
            SocialBackupCryptoError.InvalidFormat("Unsupported cipher: ${data.crypto.cipher}")
        }

        val nonce = ensureNotNull(data.crypto.cipherparams.nonce.hexToBytesOrNull()) {
            SocialBackupCryptoError.InvalidFormat("Malformed hex: nonce")
        }
        ensure(nonce.size == GCM_NONCE_SIZE_BYTES) {
            SocialBackupCryptoError.InvalidFormat("Invalid nonce size: ${nonce.size}")
        }
        val ciphertext = ensureNotNull(data.crypto.ciphertext.hexToBytesOrNull()) {
            SocialBackupCryptoError.InvalidFormat("Malformed hex: ciphertext")
        }
        ensure(ciphertext.isNotEmpty()) { SocialBackupCryptoError.InvalidFormat("Empty ciphertext") }
        val tag = ensureNotNull(data.crypto.tag.hexToBytesOrNull()) {
            SocialBackupCryptoError.InvalidFormat("Malformed hex: tag")
        }
        ensure(tag.size == GCM_TAG_SIZE_BYTES) {
            SocialBackupCryptoError.InvalidFormat("Invalid tag size: ${tag.size}")
        }

        return DecodedCryptoParams(nonce = nonce, ciphertext = ciphertext, tag = tag)
    }

    /** Rejected here rather than left to blow up inside `UserWalletId`, which the caller builds from it */
    private fun Raise<SocialBackupCryptoError>.ensureReadableWalletId(data: SocialBackupFileData) {
        ensure(data.walletId.isNotEmpty() && data.walletId.hexToBytesOrNull() != null) {
            SocialBackupCryptoError.InvalidFormat("Malformed hex: walletId")
        }
    }

    private class DecodedCryptoParams(
        val nonce: ByteArray,
        val ciphertext: ByteArray,
        val tag: ByteArray,
    )

    private fun aad(version: Int, walletId: String): ByteArray =
        "$version$AAD_SEPARATOR$walletId".toByteArray(Charsets.UTF_8)

    private fun aesGcm(mode: Int, key: ByteArray, nonce: ByteArray, input: ByteArray, aad: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(mode, SecretKeySpec(key, KEY_ALGORITHM_AES), GCMParameterSpec(GCM_TAG_SIZE_BITS, nonce))
        cipher.updateAAD(aad)
        return cipher.doFinal(input)
    }

    private fun ByteArray.toHex(): String = joinToString(separator = "") { byte ->
        "%02x".format(byte.toInt() and BYTE_MASK)
    }

    private fun String.hexToBytesOrNull(): ByteArray? {
        // ASCII 0-9/a-f only: Char.isDigit() also accepts non-ASCII Unicode digits, which then blow up
        // toInt(radix = 16) with an uncaught NumberFormatException
        val isValid = length % 2 == 0 && all { it in '0'..'9' || it.lowercaseChar() in 'a'..'f' }
        if (!isValid) return null
        return chunked(size = 2) { it.toString().toInt(radix = 16).toByte() }.toByteArray()
    }

    companion object {
        const val CIPHER_AES_256_GCM = "aes-256-gcm"
        const val VERSION = 1

        private const val GCM_NONCE_SIZE_BYTES = 12
        private const val GCM_TAG_SIZE_BITS = 128
        private const val GCM_TAG_SIZE_BYTES = GCM_TAG_SIZE_BITS / 8
        private const val KEY_ALGORITHM_AES = "AES"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val BYTE_MASK = 0xFF
        private const val AAD_SEPARATOR = "."
    }
}