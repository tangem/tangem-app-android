package com.tangem.data.cloudbackup.crypto

import com.google.common.truth.Truth.assertThat
import com.tangem.data.cloudbackup.CloudBackupJson
import org.junit.jupiter.api.Test

internal class CloudBackupFileDataTest {

    @Test
    fun `GIVEN keystore json WHEN decoded THEN all fields parsed`() {
        // Arrange
        val json = """
            {
                "crypto" : {
                    "cipher" : "aes-256-gcm",
                    "cipherparams" : {
                        "nonce" : "83dbcc02d8ccb40e466191a1"
                    },
                    "ciphertext" : "d172bf743a674da9cdad04534d56926ef8358534d458fffccd4e6ad2fbde479c",
                    "tag" : "0102030405060708090a0b0c0d0e0f10",
                    "kdf" : "argon2id",
                    "kdfparams" : {
                        "version" : 19,
                        "memory" : 65536,
                        "iterations" : 3,
                        "parallelism" : 1,
                        "dklen" : 32,
                        "salt" : "ab0c7876052600dd703518d6fc3fe8984592145b591fc8fb5c6d43190334ba19"
                    }
                },
                "id" : "3198bc9c-6672-5ab3-d995-4942343ae5b6",
                "version" : 1
            }
        """.trimIndent()

        // Act
        val actual = CloudBackupJson.decodeFromString<CloudBackupFileData>(json)

        // Assert
        val expected = CloudBackupFileData(
            version = 1,
            id = "3198bc9c-6672-5ab3-d995-4942343ae5b6",
            crypto = CloudBackupFileData.CryptoData(
                cipher = "aes-256-gcm",
                cipherparams = CloudBackupFileData.CipherParams(nonce = "83dbcc02d8ccb40e466191a1"),
                ciphertext = "d172bf743a674da9cdad04534d56926ef8358534d458fffccd4e6ad2fbde479c",
                tag = "0102030405060708090a0b0c0d0e0f10",
                kdf = "argon2id",
                kdfparams = CloudBackupFileData.KdfParams(
                    version = 19,
                    memory = 65536,
                    iterations = 3,
                    parallelism = 1,
                    dklen = 32,
                    salt = "ab0c7876052600dd703518d6fc3fe8984592145b591fc8fb5c6d43190334ba19",
                ),
            ),
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN file data with metadata WHEN encoded and decoded THEN data survives roundtrip`() {
        // Arrange
        val data = CloudBackupCipher().encrypt(
            secret = byteArrayOf(1, 2, 3),
            password = "password".toCharArray(),
            metadata = CloudBackupCipher.Metadata(
                name = "Wallet 1",
                walletId = "wallet-id-1",
                createdAt = "2026-01-09T22:13:20Z",
            ),
            params = CloudBackupCipher.Argon2Params(memoryKib = 64, iterations = 1, parallelism = 1),
        )

        // Act
        val actual = CloudBackupJson.decodeFromString<CloudBackupFileData>(CloudBackupJson.encodeToString(data))

        // Assert
        assertThat(actual).isEqualTo(data)
    }
}