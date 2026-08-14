package com.tangem.data.cloudbackup.crypto

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.security.SecureRandom

internal class CloudBackupCipherTest {

    private val cipher = CloudBackupCipher()

    @Test
    fun `GIVEN encrypted secret WHEN decrypt with same password THEN original secret returned`() {
        // Arrange
        val secret = "some mnemonic words for the backup payload".toByteArray()
        val password = "correct horse battery staple".toCharArray()
        val encrypted = cipher.encrypt(secret, password, METADATA, LIGHT_PARAMS)

        // Act
        val actual = cipher.decrypt(encrypted, password)

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(secret)
    }

    @Test
    fun `GIVEN encrypted secret WHEN decrypt with wrong password THEN WrongPassword returned`() {
        // Arrange
        val encrypted = cipher.encrypt(byteArrayOf(1, 2, 3), "password".toCharArray(), METADATA, LIGHT_PARAMS)

        // Act
        val actual = cipher.decrypt(encrypted, "wrong password".toCharArray())

        // Assert
        assertThat(actual.leftOrNull()).isEqualTo(CloudBackupCryptoError.WrongPassword)
    }

    @Test
    fun `GIVEN tampered ciphertext WHEN decrypt THEN WrongPassword returned`() {
        // Arrange
        val password = "password".toCharArray()
        val encrypted = cipher.encrypt(byteArrayOf(1, 2, 3, 4), password, METADATA, LIGHT_PARAMS)
        val tampered = encrypted.copy(
            crypto = encrypted.crypto.copy(ciphertext = encrypted.crypto.ciphertext.reversed()),
        )

        // Act
        val actual = cipher.decrypt(tampered, password)

        // Assert
        assertThat(actual.leftOrNull()).isEqualTo(CloudBackupCryptoError.WrongPassword)
    }

    @Test
    fun `GIVEN unsupported cipher WHEN decrypt THEN InvalidFormat returned`() {
        // Arrange
        val encrypted = cipher.encrypt(byteArrayOf(1), "password".toCharArray(), METADATA, LIGHT_PARAMS)
        val broken = encrypted.copy(crypto = encrypted.crypto.copy(cipher = "aes-128-ctr"))

        // Act
        val actual = cipher.decrypt(broken, "password".toCharArray())

        // Assert
        assertThat(
            actual.leftOrNull(),
        ).isEqualTo(CloudBackupCryptoError.InvalidFormat("Unsupported cipher: aes-128-ctr"))
    }

    @Test
    fun `GIVEN unsupported kdf WHEN decrypt THEN InvalidFormat returned`() {
        // Arrange
        val encrypted = cipher.encrypt(byteArrayOf(1), "password".toCharArray(), METADATA, LIGHT_PARAMS)
        val broken = encrypted.copy(crypto = encrypted.crypto.copy(kdf = "pbkdf2"))

        // Act
        val actual = cipher.decrypt(broken, "password".toCharArray())

        // Assert
        assertThat(actual.leftOrNull()).isEqualTo(CloudBackupCryptoError.InvalidFormat("Unsupported kdf: pbkdf2"))
    }

    @Test
    fun `GIVEN malformed salt WHEN decrypt THEN InvalidFormat returned`() {
        // Arrange
        val encrypted = cipher.encrypt(byteArrayOf(1), "password".toCharArray(), METADATA, LIGHT_PARAMS)
        val broken = encrypted.copy(
            crypto = encrypted.crypto.copy(kdfparams = encrypted.crypto.kdfparams.copy(salt = "not hex")),
        )

        // Act
        val actual = cipher.decrypt(broken, "password".toCharArray())

        // Assert
        assertThat(actual.leftOrNull()).isEqualTo(CloudBackupCryptoError.InvalidFormat("Malformed hex: salt"))
    }

    @Test
    fun `GIVEN negative dklen WHEN decrypt THEN InvalidFormat and no crash`() {
        // Arrange
        val encrypted = cipher.encrypt(byteArrayOf(1), "password".toCharArray(), METADATA, LIGHT_PARAMS)
        val broken = encrypted.copy(
            crypto = encrypted.crypto.copy(kdfparams = encrypted.crypto.kdfparams.copy(dklen = -1)),
        )

        // Act
        val actual = cipher.decrypt(broken, "password".toCharArray())

        // Assert
        assertThat(actual.leftOrNull()).isEqualTo(CloudBackupCryptoError.InvalidFormat("Invalid dklen: -1"))
    }

    @Test
    fun `GIVEN excessive memory WHEN decrypt THEN InvalidFormat and no OOM`() {
        // Arrange
        val encrypted = cipher.encrypt(byteArrayOf(1), "password".toCharArray(), METADATA, LIGHT_PARAMS)
        val broken = encrypted.copy(
            crypto = encrypted.crypto.copy(kdfparams = encrypted.crypto.kdfparams.copy(memory = Int.MAX_VALUE)),
        )

        // Act
        val actual = cipher.decrypt(broken, "password".toCharArray())

        // Assert
        assertThat(actual.leftOrNull())
            .isEqualTo(CloudBackupCryptoError.InvalidFormat("Invalid memory: ${Int.MAX_VALUE}"))
    }

    @Test
    fun `GIVEN unsupported argon2 version WHEN decrypt THEN InvalidFormat returned`() {
        // Arrange
        val encrypted = cipher.encrypt(byteArrayOf(1), "password".toCharArray(), METADATA, LIGHT_PARAMS)
        val broken = encrypted.copy(
            crypto = encrypted.crypto.copy(kdfparams = encrypted.crypto.kdfparams.copy(version = 99)),
        )

        // Act
        val actual = cipher.decrypt(broken, "password".toCharArray())

        // Assert
        assertThat(
            actual.leftOrNull(),
        ).isEqualTo(CloudBackupCryptoError.InvalidFormat("Unsupported argon2 version: 99"))
    }

    @Test
    fun `GIVEN two encryptions of same secret WHEN encrypt THEN salt nonce and id differ`() {
        // Arrange
        val secret = byteArrayOf(1, 2, 3)
        val password = "password".toCharArray()

        // Act
        val first = cipher.encrypt(secret, password, METADATA, LIGHT_PARAMS)
        val second = cipher.encrypt(secret, password, METADATA, LIGHT_PARAMS)

        // Assert
        assertThat(first.crypto.kdfparams.salt).isNotEqualTo(second.crypto.kdfparams.salt)
        assertThat(first.crypto.cipherparams.nonce).isNotEqualTo(second.crypto.cipherparams.nonce)
        assertThat(first.id).isNotEqualTo(second.id)
    }

    @Test
    fun `GIVEN password in NFD form WHEN decrypt with NFC form THEN succeeds (CPR-06)`() {
        // Arrange: the same passphrase in two Unicode normalization forms
        val nfcPassword = charArrayOf('c', 'a', 'f', '\u00e9') // precomposed e-acute (NFC)
        val nfdPassword = charArrayOf('c', 'a', 'f', 'e', '\u0301') // e + combining acute (NFD)
        val secret = byteArrayOf(1, 2, 3)
        val encrypted = cipher.encrypt(secret, nfcPassword, METADATA, LIGHT_PARAMS)

        // Act
        val actual = cipher.decrypt(encrypted, nfdPassword)

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(secret)
    }

    @Test
    fun `GIVEN metadata WHEN encrypt THEN plaintext metadata fields populated on file`() {
        // Act
        val encrypted = cipher.encrypt(byteArrayOf(1), "password".toCharArray(), METADATA, LIGHT_PARAMS)

        // Assert
        assertThat(encrypted.name).isEqualTo(METADATA.name)
        assertThat(encrypted.walletId).isEqualTo(METADATA.walletId)
        assertThat(encrypted.createdAt).isEqualTo(METADATA.createdAt)
    }

    @Test
    fun `GIVEN fixed salt nonce and password WHEN encrypt THEN matches the pinned v1 vector`() {
        // Arrange — deterministic RNG pins salt/nonce; this is the v1 cross-platform format vector (must match iOS)
        val deterministicCipher = CloudBackupCipher(random = SequentialRandom())
        val secret = "backup payload".toByteArray(Charsets.UTF_8)
        val password = "correct horse battery staple".toCharArray()

        // Act
        val actual = deterministicCipher.encrypt(secret, password, METADATA, LIGHT_PARAMS)

        // Assert
        assertThat(actual.crypto).isEqualTo(
            CloudBackupFileData.CryptoData(
                cipher = "aes-256-gcm",
                cipherparams = CloudBackupFileData.CipherParams(nonce = "101112131415161718191a1b"),
                ciphertext = "a1b6d2aba89a9a59bb02b594528c",
                tag = "5affe455a7b932c1123bbadcf77778cf",
                kdf = "argon2id",
                kdfparams = CloudBackupFileData.KdfParams(
                    version = 19,
                    memory = 64,
                    iterations = 1,
                    parallelism = 1,
                    dklen = 32,
                    salt = "000102030405060708090a0b0c0d0e0f",
                ),
            ),
        )
    }

    private class SequentialRandom : SecureRandom() {
        private var counter = 0
        override fun nextBytes(bytes: ByteArray) {
            for (i in bytes.indices) {
                bytes[i] = counter.toByte()
                counter++
            }
        }
    }

    private companion object {
        /** Low-cost Argon2id params to keep unit tests fast, not used in production */
        val LIGHT_PARAMS = CloudBackupCipher.Argon2Params(memoryKib = 64, iterations = 1, parallelism = 1)
        val METADATA = CloudBackupCipher.Metadata(
            name = "Wallet",
            walletId = "wallet-1",
            createdAt = "2024-01-15T10:30:00Z",
        )
    }
}