package com.tangem.data.polymarket.signer

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
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
            ownerAddress = OWNER,
            credentials = CREDENTIALS,
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
            ownerAddress = OWNER,
            credentials = CREDENTIALS,
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
            ownerAddress = OWNER,
            credentials = CREDENTIALS,
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
            ownerAddress = OWNER,
            credentials = CREDENTIALS,
            timestamp = "1700000000",
            method = "GET",
            requestPath = "/order",
        )

        // Assert
        assertThat(actual.values.none { it.contains(CREDENTIALS.secret) }).isTrue()
    }

    private companion object {
        const val OWNER = "0xabc"
        val CREDENTIALS = PolymarketApiCredentials(
            apiKey = "k",
            secret = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=",
            passphrase = "p",
        )
    }
}