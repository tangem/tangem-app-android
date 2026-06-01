package com.tangem.lib.auth.dpop.internal

import arrow.core.None
import arrow.core.Some
import com.google.common.truth.Truth.assertThat
import com.tangem.lib.auth.devicekey.DeviceKeyManager
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultDpopProofFactoryTest {

    private val dispatchers = TestingCoroutineDispatcherProvider()
    private val deviceKeyManager: DeviceKeyManager = mockk()
    private val json = Json.Default

    // Fixed P-256 public key (uncompressed): 0x04 || X(32) || Y(32). Values are arbitrary but
    // span both halves so any off-by-one slice mistake is caught.
    private val devicePublicKey: ByteArray = byteArrayOf(0x04) +
        ByteArray(COORDINATE_SIZE) { it.toByte() } +
        ByteArray(COORDINATE_SIZE) { (it + COORDINATE_SIZE).toByte() }

    private val signatureBytes: ByteArray = ByteArray(SIGNATURE_SIZE) { (it + 1).toByte() }

    private val fixedInstant = Instant.fromEpochSeconds(1_700_000_000)
    private val fixedJti = UUID.fromString("11111111-2222-3333-4444-555555555555")

    private lateinit var factory: DefaultDpopProofFactory

    @BeforeEach
    fun setup() {
        // android.util.Base64 → java.util.Base64
        mockkStatic(android.util.Base64::class)
        every { android.util.Base64.encodeToString(any(), any()) } answers {
            val bytes = firstArg<ByteArray>()
            val flags = secondArg<Int>()
            val padded = flags and android.util.Base64.NO_PADDING == 0
            val encoder = if (flags and android.util.Base64.URL_SAFE != 0) {
                if (padded) Base64.getUrlEncoder() else Base64.getUrlEncoder().withoutPadding()
            } else {
                Base64.getEncoder()
            }
            encoder.encodeToString(bytes)
        }

        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returns fixedJti

        coEvery { deviceKeyManager.getPublicKey() } returns Some(devicePublicKey)
        coEvery { deviceKeyManager.sign(any()) } returns signatureBytes

        factory = DefaultDpopProofFactory(
            deviceKeyManager = deviceKeyManager,
            json = json,
            clock = object : Clock { override fun now(): Instant = fixedInstant },
            dispatchers = dispatchers,
        )
    }

    @AfterEach
    fun teardown() = unmockkAll()

    @Test
    fun `create produces JWS with ath when access token is provided`() = runTest {
        val token = "header.payload.signature"
        val proof = factory.create("post", "https://example.com/api/v1/auth/refresh?ignored=1#frag", token)
            .getOrNull()!!

        val parts = proof.split('.')
        assertThat(parts).hasSize(3)

        val header = decodeJsonObject(parts[0])
        assertThat(header["alg"]?.jsonPrimitive?.contentOrNull).isEqualTo("ES256")
        assertThat(header["typ"]?.jsonPrimitive?.contentOrNull).isEqualTo("dpop+jwt")
        val jwk = header["jwk"]!!.jsonObject
        assertThat(jwk["kty"]?.jsonPrimitive?.contentOrNull).isEqualTo("EC")
        assertThat(jwk["crv"]?.jsonPrimitive?.contentOrNull).isEqualTo("P-256")
        assertThat(jwk["x"]?.jsonPrimitive?.contentOrNull)
            .isEqualTo(base64UrlNoPad(devicePublicKey.sliceArray(1..COORDINATE_SIZE)))
        assertThat(jwk["y"]?.jsonPrimitive?.contentOrNull)
            .isEqualTo(base64UrlNoPad(devicePublicKey.sliceArray(COORDINATE_SIZE + 1..2 * COORDINATE_SIZE)))

        val claims = decodeJsonObject(parts[1])
        assertThat(claims["jti"]?.jsonPrimitive?.contentOrNull).isEqualTo(fixedJti.toString())
        assertThat(claims["iat"]?.jsonPrimitive?.longOrNull).isEqualTo(fixedInstant.epochSeconds)
        assertThat(claims["htm"]?.jsonPrimitive?.contentOrNull).isEqualTo("POST")
        assertThat(claims["htu"]?.jsonPrimitive?.contentOrNull).isEqualTo("https://example.com/api/v1/auth/refresh")
        assertThat(claims["ath"]?.jsonPrimitive?.contentOrNull)
            .isEqualTo(base64UrlNoPad(sha256(token.toByteArray(Charsets.US_ASCII))))

        assertThat(parts[2]).isEqualTo(base64UrlNoPad(signatureBytes))
    }

    @Test
    fun `create omits ath when access token is null`() = runTest {
        val proof = factory.create("POST", "https://example.com/refresh", null).getOrNull()!!

        val claims = decodeJsonObject(proof.split('.')[1])
        assertThat(claims.containsKey("ath")).isFalse()
        assertThat(claims["htm"]?.jsonPrimitive?.contentOrNull).isEqualTo("POST")
    }

    @Test
    fun `htu preserves percent-encoded characters in path`() = runTest {
        // DPoP verification is byte-sensitive: %2F must NOT be decoded to / in htu.
        val proof = factory.create("GET", "https://api.example.com/wallet%2F123/sub?x=1", null).getOrNull()!!

        val claims = decodeJsonObject(proof.split('.')[1])
        assertThat(claims["htu"]?.jsonPrimitive?.contentOrNull)
            .isEqualTo("https://api.example.com/wallet%2F123/sub")
    }

    @Test
    fun `create returns None when device key unavailable`() = runTest {
        coEvery { deviceKeyManager.getPublicKey() } returns None

        val result = factory.create("POST", "https://example.com", null)

        assertThat(result).isEqualTo(None)
    }

    @Test
    fun `create signs the b64u-encoded header dot payload`() = runTest {
        factory.create("GET", "https://example.com", null)

        // The signing input is `<header_b64>.<claims_b64>` — verify it has a dot separator and
        // a non-empty header section.
        coVerify {
            deviceKeyManager.sign(match { bytes ->
                val text = String(bytes, Charsets.US_ASCII)
                text.contains('.') && text.substringBefore('.').isNotEmpty()
            })
        }
    }

    private fun decodeJsonObject(b64: String): JsonObject =
        json.parseToJsonElement(String(Base64.getUrlDecoder().decode(b64), Charsets.UTF_8)).jsonObject

    private fun base64UrlNoPad(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    private fun sha256(bytes: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(bytes)

    private companion object {
        const val COORDINATE_SIZE = 32
        const val SIGNATURE_SIZE = 64
    }
}