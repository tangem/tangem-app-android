package com.tangem.lib.auth.devicekey.internal

import arrow.core.None
import com.google.common.truth.Truth.assertThat
import com.tangem.lib.auth.devicekey.DeviceKeySigningException
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.Certificate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultDeviceKeyManagerTest {

    private val keyStore: KeyStore = mockk(relaxed = true)
    private val dispatchers = TestingCoroutineDispatcherProvider()
    private val manager: DefaultDeviceKeyManager = DefaultDeviceKeyManager(keyStore, dispatchers)

    @BeforeEach
    fun setup() {
        clearMocks(keyStore)
    }

    @AfterEach
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `generateIfMissing returns false when key already exists`() = runTest {
        every { keyStore.containsAlias(KEY_ALIAS) } returns true

        val result = manager.generateIfMissing()

        assertThat(result).isFalse()
    }

    @Test
    fun `generateIfMissing returns false when generation fails`() = runTest {
        every { keyStore.containsAlias(KEY_ALIAS) } returns false

        val keyPairGenerator = mockk<KeyPairGenerator>(relaxed = true)
        every { keyPairGenerator.generateKeyPair() } throws RuntimeException("keystore unavailable")

        mockkStatic(KeyPairGenerator::class)
        every { KeyPairGenerator.getInstance("EC", "AndroidKeyStore") } returns keyPairGenerator

        val result = manager.generateIfMissing()

        assertThat(result).isFalse()
    }

    @Test
    fun `getPublicKey returns last 65 bytes from encoded key`() = runTest {
        val rawPoint = ByteArray(65) { (it + 1).toByte() }.apply { this[0] = 0x04 }
        val x509Header = ByteArray(26) { 0x30 }
        val encoded = x509Header + rawPoint

        val publicKey = mockk<java.security.PublicKey>()
        every { publicKey.encoded } returns encoded

        val cert = mockk<Certificate>()
        every { cert.publicKey } returns publicKey
        every { keyStore.getCertificate(KEY_ALIAS) } returns cert

        val result = manager.getPublicKey()

        assertThat(result.getOrNull()).isEqualTo(rawPoint)
    }

    @Test
    fun `getPublicKey returns None when certificate not found`() = runTest {
        every { keyStore.getCertificate(KEY_ALIAS) } returns null

        val result = manager.getPublicKey()

        assertThat(result).isEqualTo(None)
    }

    @Test
    fun `getPublicKey returns None when point prefix is not uncompressed`() = runTest {
        val rawPoint = ByteArray(65) { (it + 1).toByte() }.apply { this[0] = 0x02 }
        val x509Header = ByteArray(26) { 0x30 }
        val encoded = x509Header + rawPoint

        val publicKey = mockk<java.security.PublicKey>()
        every { publicKey.encoded } returns encoded

        val cert = mockk<Certificate>()
        every { cert.publicKey } returns publicKey
        every { keyStore.getCertificate(KEY_ALIAS) } returns cert

        val result = manager.getPublicKey()

        assertThat(result).isEqualTo(None)
    }

    @Test
    fun `sign returns raw 64-byte signature`() = runTest {
        val data = "test data".toByteArray()
        val r = ByteArray(32) { 0x01 }
        val s = ByteArray(32) { 0x02 }
        val derSignature = buildDer(r, s)

        val privateKey = mockk<PrivateKey>()
        every { keyStore.getKey(KEY_ALIAS, null) } returns privateKey

        val javaSig = mockk<java.security.Signature>()
        every { javaSig.initSign(privateKey) } returns Unit
        every { javaSig.update(data) } returns Unit
        every { javaSig.sign() } returns derSignature

        mockkSignatureGetInstance(javaSig)

        val result = manager.sign(data)

        assertThat(result).hasLength(64)
        assertThat(result.copyOfRange(0, 32)).isEqualTo(r)
        assertThat(result.copyOfRange(32, 64)).isEqualTo(s)
    }

    @Test
    fun `sign throws DeviceKeySigningException when key not found`() = runTest {
        every { keyStore.getKey(KEY_ALIAS, null) } returns null

        val exception = assertThrows<DeviceKeySigningException> {
            manager.sign("data".toByteArray())
        }
        assertThat(exception.message).contains("Device key not found")
    }

    @Test
    fun `sign wraps unexpected exception in DeviceKeySigningException`() = runTest {
        val privateKey = mockk<PrivateKey>()
        every { keyStore.getKey(KEY_ALIAS, null) } returns privateKey

        val javaSig = mockk<java.security.Signature>()
        every { javaSig.initSign(privateKey) } throws RuntimeException("hardware error")

        mockkSignatureGetInstance(javaSig)

        val exception = assertThrows<DeviceKeySigningException> {
            manager.sign("data".toByteArray())
        }
        assertThat(exception.message).isEqualTo("Signing failed")
        assertThat(exception.cause).isInstanceOf(RuntimeException::class.java)
    }

    private fun mockkSignatureGetInstance(mock: java.security.Signature) {
        io.mockk.mockkStatic(java.security.Signature::class)
        every { java.security.Signature.getInstance("SHA256withECDSA") } returns mock
    }

    private fun buildDer(r: ByteArray, s: ByteArray): ByteArray {
        val rTlv = byteArrayOf(0x02, r.size.toByte()) + r
        val sTlv = byteArrayOf(0x02, s.size.toByte()) + s
        val body = rTlv + sTlv
        return byteArrayOf(0x30, body.size.toByte()) + body
    }

    private companion object {
        const val KEY_ALIAS = "tangem_device_key"
    }
}