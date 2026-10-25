package com.tangem.data.polymarket.signer

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import com.tangem.domain.polymarket.model.PolymarketClobIdentity
import org.junit.jupiter.api.Test
import java.util.Base64 as JavaBase64

internal class PolymarketL2HeaderBuilderTest {

    private val jvmCodec = object : Base64UrlCodec {
        override fun decode(value: String): ByteArray = JavaBase64.getUrlDecoder().decode(value)
        override fun encode(bytes: ByteArray): String = JavaBase64.getUrlEncoder().encodeToString(bytes)
    }

    private val signer = PolymarketHmacSigner(jvmCodec)
    private val builder = PolymarketL2HeaderBuilder(signer)

    @Test
    fun `GIVEN GET request WHEN build THEN produces the five POLY headers with the expected signature`() {
        // Act
        val actual = builder.build(
            identity = OWNER_IDENTITY,
            timestamp = "1700000000",
            method = "GET",
            requestPath = "/order",
        )

        // Assert
        assertThat(actual).containsExactlyEntriesIn(
            mapOf(
                "POLY_ADDRESS" to OWNER,
                "POLY_SIGNATURE" to "pE_JGF8a0_X6nTMMD8ZNRmDXwtPNiD7XFQXAlTRuN8Q=",
                "POLY_TIMESTAMP" to "1700000000",
                "POLY_API_KEY" to "k",
                "POLY_PASSPHRASE" to "p",
            ),
        )
    }

    @Test
    fun `GIVEN POST request with body WHEN build THEN the signed message includes the body`() {
        // Act
        val actual = builder.build(
            identity = OWNER_IDENTITY,
            timestamp = "1700000000",
            method = "POST",
            requestPath = "/order",
            body = "{\"a\":1}",
        )

        // Assert
        assertThat(actual["POLY_SIGNATURE"]).isEqualTo("uBmRP7BjQtWls1fT877WgPByC-SiSnPQJFihV7H5eUM=")
    }

    @Test
    fun `GIVEN the balance sync path WHEN build THEN signs the path with no query string`() {
        // Act
        val actual = builder.build(
            identity = OWNER_IDENTITY,
            timestamp = "1700000000",
            method = "GET",
            requestPath = "/balance-allowance/update",
        )

        // Assert
        assertThat(actual["POLY_SIGNATURE"]).isEqualTo("HYThrzuMjEOc4dkcb09p7taGRQiJyCU75VyT_cIrXnE=")
    }

    @Test
    fun `GIVEN credentials WHEN build THEN the secret never appears in any header value`() {
        // Act
        val actual = builder.build(
            identity = OWNER_IDENTITY,
            timestamp = "1700000000",
            method = "GET",
            requestPath = "/order",
        )

        // Assert
        assertThat(actual.values.none { it.contains(CREDENTIALS.secret) }).isTrue()
    }

    @Test
    fun `GIVEN no method or timestamp WHEN build THEN signs GET with the current time in seconds`() {
        // Arrange
        val nowSeconds = System.currentTimeMillis() / 1_000L

        // Act
        val actual = builder.build(
            identity = OWNER_IDENTITY,
            requestPath = "/balance-allowance/update",
        )

        // Assert
        val timestamp = actual.getValue("POLY_TIMESTAMP")
        assertThat(timestamp.toLong()).isAtLeast(nowSeconds - 60)
        assertThat(timestamp.toLong()).isAtMost(nowSeconds + 60)
        assertThat(actual["POLY_SIGNATURE"]).isEqualTo(
            signer.sign(secret = CREDENTIALS.secret, message = timestamp + "GET" + "/balance-allowance/update"),
        )
    }

    @Test
    fun `GIVEN a session identity WHEN build THEN POLY_ADDRESS is the session key and its own key signs`() {
        // Act
        val actual = builder.build(
            identity = PolymarketClobIdentity.Session(address = SESSION_KEY, credentials = SESSION_CREDENTIALS),
            timestamp = "1700000000",
            method = "GET",
            requestPath = "/order",
        )

        // Assert
        assertThat(actual).containsExactlyEntriesIn(
            mapOf(
                "POLY_ADDRESS" to SESSION_KEY,
                "POLY_SIGNATURE" to signer.sign(
                    secret = SESSION_CREDENTIALS.secret,
                    message = "1700000000" + "GET" + "/order",
                ),
                "POLY_TIMESTAMP" to "1700000000",
                "POLY_API_KEY" to "session-k",
                "POLY_PASSPHRASE" to "session-p",
            ),
        )
    }

    private companion object {
        const val OWNER = "0xabc"
        const val SESSION_KEY = "0xdef"
        val CREDENTIALS = PolymarketApiCredentials(
            apiKey = "k",
            secret = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=",
            passphrase = "p",
        )
        val SESSION_CREDENTIALS = PolymarketApiCredentials(
            apiKey = "session-k",
            secret = "HxwdHhkaGxwVFhcYERITFA0ODxAJCgsMBQYHCAECAwQ=",
            passphrase = "session-p",
        )
        val OWNER_IDENTITY = PolymarketClobIdentity.Owner(address = OWNER, credentials = CREDENTIALS)
    }
}