package com.tangem.lib.auth.nonce.internal

import com.google.common.truth.Truth.assertThat
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import java.security.KeyPairGenerator
import java.security.spec.MGF1ParameterSpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultAuthNonceDecryptorTest {

    private val dispatchers = TestingCoroutineDispatcherProvider()
    private val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

    private val privateKeyBase64: String =
        Base64.getEncoder().encodeToString(keyPair.private.encoded)

    private lateinit var decryptor: DefaultAuthNonceDecryptor

    @BeforeEach
    fun setup() {
        mockkStatic(android.util.Base64::class)
        every { android.util.Base64.decode(any<String>(), any()) } answers {
            val input = firstArg<String>()
            val flags = secondArg<Int>()
            if (flags and android.util.Base64.URL_SAFE != 0) {
                Base64.getUrlDecoder().decode(input)
            } else {
                Base64.getDecoder().decode(input)
            }
        }
        decryptor = DefaultAuthNonceDecryptor(privateKeyBase64, dispatchers)
    }

    @AfterEach
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `decryptNonce returns original nonce string`() = runTest {
        val nonce = "dGVzdC1ub25jZS0xMjM0NQ"
        val encrypted = encryptAndEncodeBase64Url(nonce)

        val result = decryptor.decryptNonce(encrypted)

        assertThat(result).isEqualTo(nonce)
    }

    @Test
    fun `decryptNonce handles base64url nonce from backend`() = runTest {
        val randomBytes = ByteArray(32) { it.toByte() }
        val nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes)
        val encrypted = encryptAndEncodeBase64Url(nonce)

        val result = decryptor.decryptNonce(encrypted)

        assertThat(result).isEqualTo(nonce)
    }

    @Test
    fun `constructor throws on invalid key`() {
        assertThrows<Exception> {
            DefaultAuthNonceDecryptor("not-a-valid-base64-key!!", dispatchers)
        }
    }

    @Test
    fun `decryptNonce throws on corrupted ciphertext`() = runTest {
        val corrupted = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(256) { 0x42 })

        assertThrows<Exception> {
            decryptor.decryptNonce(corrupted)
        }
    }

    private fun encryptAndEncodeBase64Url(plainNonce: String): String {
        val oaepSpec = OAEPParameterSpec(
            "SHA-256",
            "MGF1",
            MGF1ParameterSpec.SHA256,
            PSource.PSpecified.DEFAULT,
        )
        val cipher = Cipher.getInstance("RSA/ECB/OAEPPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keyPair.public, oaepSpec)
        val encrypted = cipher.doFinal(plainNonce.toByteArray(Charsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(encrypted)
    }
}