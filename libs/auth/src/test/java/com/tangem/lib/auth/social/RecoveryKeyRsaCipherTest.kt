package com.tangem.lib.auth.social

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.MGF1ParameterSpec
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import java.util.Base64 as JavaBase64

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RecoveryKeyRsaCipherTest {

    private val keyPair = KeyPairGenerator.getInstance("RSA")
        .apply { initialize(KEY_SIZE_BITS) }
        .generateKeyPair()

    private val publicKey = keyPair.public as RSAPublicKey
    private val privateKey = keyPair.private as RSAPrivateKey

    @BeforeEach
    fun setup() {
        // android.util.Base64 → java.util.Base64
        mockkStatic(android.util.Base64::class)
        every { android.util.Base64.decode(any<String>(), any()) } answers {
            val value = firstArg<String>()
            val flags = secondArg<Int>()
            val decoder = if (flags and android.util.Base64.URL_SAFE != 0) {
                JavaBase64.getUrlDecoder()
            } else {
                JavaBase64.getDecoder()
            }
            decoder.decode(value)
        }
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(android.util.Base64::class)
    }

    @Test
    fun `GIVEN an RSA JWK WHEN seal THEN the key holder recovers the plaintext`() {
        // Arrange
        val recoveryKey = ByteArray(RECOVERY_KEY_SIZE_BYTES) { it.toByte() }

        // Act
        val sealed = RecoveryKeyRsaCipher.seal(
            modulusBase64Url = publicKey.modulus.toBase64Url(),
            exponentBase64Url = publicKey.publicExponent.toBase64Url(),
            plaintext = recoveryKey,
        )

        // Assert
        assertThat(sealed).hasLength(KEY_SIZE_BITS / Byte.SIZE_BITS)
        assertThat(decryptWithOaep256(sealed)).isEqualTo(recoveryKey)
    }

    @Test
    fun `GIVEN the same plaintext WHEN sealed twice THEN the ciphertexts differ`() {
        // Arrange
        val recoveryKey = ByteArray(RECOVERY_KEY_SIZE_BYTES) { 7 }
        val modulus = publicKey.modulus.toBase64Url()
        val exponent = publicKey.publicExponent.toBase64Url()

        // Act
        val first = RecoveryKeyRsaCipher.seal(modulus, exponent, recoveryKey)
        val second = RecoveryKeyRsaCipher.seal(modulus, exponent, recoveryKey)

        // Assert
        assertThat(first).isNotEqualTo(second)
    }

    /** OAEP with SHA-256 for both the digest and MGF1 — the JCE default would use MGF1-SHA-1 here */
    private fun decryptWithOaep256(ciphertext: ByteArray): ByteArray = Cipher
        .getInstance("RSA/ECB/OAEPPadding")
        .run {
            init(
                Cipher.DECRYPT_MODE,
                privateKey,
                OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT),
            )
            doFinal(ciphertext)
        }

    /** JWK numbers are unsigned big-endian, so a leading zero sign byte must be dropped */
    private fun java.math.BigInteger.toBase64Url(): String = JavaBase64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(toByteArray().dropWhile { it == 0.toByte() }.toByteArray())

    private companion object {
        const val KEY_SIZE_BITS = 2048
        const val RECOVERY_KEY_SIZE_BYTES = 32
    }
}