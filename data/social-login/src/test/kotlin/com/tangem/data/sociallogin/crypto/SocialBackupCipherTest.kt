package com.tangem.data.sociallogin.crypto

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.sociallogin.models.RecoveryKey
import org.junit.jupiter.api.Test
import java.security.SecureRandom

internal class SocialBackupCipherTest {

    private val cipher = SocialBackupCipher()

    @Test
    fun `GIVEN payload encrypted with a key WHEN decrypted with the same key THEN the payload is recovered`() {
        // Arrange
        val key = recoveryKey()
        val payload = PAYLOAD.toByteArray()

        // Act
        val file = cipher.encrypt(payload, key, WALLET_ID, CREATED_AT)
        val decrypted = cipher.decrypt(file, key)

        // Assert
        assertThat(decrypted.getOrNull()?.decodeToString()).isEqualTo(PAYLOAD)
    }

    @Test
    fun `GIVEN the same key and payload WHEN encrypted twice THEN nonce and ciphertext differ`() {
        // Arrange
        val key = recoveryKey()
        val payload = PAYLOAD.toByteArray()

        // Act
        val first = cipher.encrypt(payload, key, WALLET_ID, CREATED_AT)
        val second = cipher.encrypt(payload, key, WALLET_ID, CREATED_AT)

        // Assert
        assertThat(first.crypto.cipherparams.nonce).isNotEqualTo(second.crypto.cipherparams.nonce)
        assertThat(first.crypto.ciphertext).isNotEqualTo(second.crypto.ciphertext)
    }

    @Test
    fun `GIVEN a file WHEN decrypted with another key THEN it is rejected`() {
        // Arrange
        val file = cipher.encrypt(PAYLOAD.toByteArray(), recoveryKey(), WALLET_ID, CREATED_AT)

        // Act
        val decrypted = cipher.decrypt(file, recoveryKey())

        // Assert
        assertThat(decrypted.leftOrNull()).isInstanceOf(SocialBackupCryptoError.InvalidFormat::class.java)
    }

    @Test
    fun `GIVEN a file whose walletId was edited WHEN decrypted THEN authentication fails`() {
        // Arrange
        val key = recoveryKey()
        val file = cipher.encrypt(PAYLOAD.toByteArray(), key, WALLET_ID, CREATED_AT)

        // Act
        val decrypted = cipher.decrypt(file.copy(walletId = OTHER_WALLET_ID), key)

        // Assert
        assertThat(decrypted.leftOrNull()).isInstanceOf(SocialBackupCryptoError.InvalidFormat::class.java)
    }

    @Test
    fun `GIVEN a file whose ciphertext was edited WHEN decrypted THEN authentication fails`() {
        // Arrange
        val key = recoveryKey()
        val file = cipher.encrypt(PAYLOAD.toByteArray(), key, WALLET_ID, CREATED_AT)
        val tampered = file.copy(
            crypto = file.crypto.copy(ciphertext = file.crypto.ciphertext.flipFirstHexDigit()),
        )

        // Act
        val decrypted = cipher.decrypt(tampered, key)

        // Assert
        assertThat(decrypted.leftOrNull()).isInstanceOf(SocialBackupCryptoError.InvalidFormat::class.java)
    }

    @Test
    fun `GIVEN a file of an unsupported version WHEN decrypted THEN it is rejected before decryption`() {
        // Arrange
        val key = recoveryKey()
        val file = cipher.encrypt(PAYLOAD.toByteArray(), key, WALLET_ID, CREATED_AT)

        // Act
        val decrypted = cipher.decrypt(file.copy(version = UNSUPPORTED_VERSION), key)

        // Assert
        assertThat(decrypted.leftOrNull()).isEqualTo(SocialBackupCryptoError.UnsupportedVersion(UNSUPPORTED_VERSION))
    }

    @Test
    fun `GIVEN a file with a malformed header WHEN validated THEN it is rejected`() {
        // Arrange
        val file = cipher.encrypt(PAYLOAD.toByteArray(), recoveryKey(), WALLET_ID, CREATED_AT)
        val malformed = listOf(
            file.copy(walletId = "not hex"),
            file.copy(crypto = file.crypto.copy(cipher = "aes-128-gcm")),
            file.copy(crypto = file.crypto.copy(cipherparams = SocialBackupFileData.CipherParams(nonce = "00"))),
            file.copy(crypto = file.crypto.copy(tag = "00")),
            file.copy(crypto = file.crypto.copy(ciphertext = "zz")),
        )

        // Act & Assert
        malformed.forEach { data ->
            assertThat(cipher.validateFormat(data).leftOrNull())
                .isInstanceOf(SocialBackupCryptoError.InvalidFormat::class.java)
        }
    }

    @Test
    fun `GIVEN a freshly written file WHEN inspected THEN it declares the current format`() {
        // Arrange & Act
        val file = cipher.encrypt(PAYLOAD.toByteArray(), recoveryKey(), WALLET_ID, CREATED_AT)

        // Assert
        assertThat(file.version).isEqualTo(SocialBackupCipher.VERSION)
        assertThat(file.crypto.cipher).isEqualTo(SocialBackupCipher.CIPHER_AES_256_GCM)
        assertThat(file.walletId).isEqualTo(WALLET_ID)
        assertThat(file.createdAt).isEqualTo(CREATED_AT)
        assertThat(file.crypto.cipherparams.nonce).hasLength(NONCE_HEX_LENGTH)
        assertThat(file.crypto.tag).hasLength(TAG_HEX_LENGTH)
    }

    private fun recoveryKey(): RecoveryKey =
        RecoveryKey(ByteArray(RecoveryKey.SIZE_BYTES).also(SecureRandom()::nextBytes))

    private fun String.flipFirstHexDigit(): String = (if (first() == '0') '1' else '0') + drop(n = 1)

    private companion object {
        const val WALLET_ID = "104E6A9ECD85FFDF5F94E2CE4AC90B8CF6AA47CC81A0FE213359D3604F18769B"
        const val OTHER_WALLET_ID = "204E6A9ECD85FFDF5F94E2CE4AC90B8CF6AA47CC81A0FE213359D3604F18769B"
        const val CREATED_AT = "2026-08-10T10:15:30Z"
        const val PAYLOAD = """{"mnemonic":"one two three","passphraseRequired":0}"""
        const val UNSUPPORTED_VERSION = 2
        const val NONCE_HEX_LENGTH = 24
        const val TAG_HEX_LENGTH = 32
    }
}