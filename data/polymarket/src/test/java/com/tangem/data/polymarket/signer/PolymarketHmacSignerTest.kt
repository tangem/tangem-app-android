package com.tangem.data.polymarket.signer

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.util.Base64 as JavaBase64

internal class PolymarketHmacSignerTest {

    private val jvmCodec = object : Base64UrlCodec {
        override fun decode(value: String): ByteArray = JavaBase64.getUrlDecoder().decode(value)
        override fun encode(bytes: ByteArray): String = JavaBase64.getUrlEncoder().encodeToString(bytes)
    }

    private val signer = PolymarketHmacSigner(jvmCodec)

    @Test
    fun `GIVEN GET message WHEN sign THEN matches the known-answer vector`() {
        // Act
        val actual = signer.sign(SECRET, "1700000000GET/order")

        // Assert
        assertThat(actual).isEqualTo("pE_JGF8a0_X6nTMMD8ZNRmDXwtPNiD7XFQXAlTRuN8Q=")
    }

    @Test
    fun `GIVEN POST message with body WHEN sign THEN matches the known-answer vector`() {
        // Act
        val actual = signer.sign(SECRET, "1700000000POST/order{\"a\":1}")

        // Assert
        assertThat(actual).isEqualTo("uBmRP7BjQtWls1fT877WgPByC-SiSnPQJFihV7H5eUM=")
    }

    @Test
    fun `GIVEN any message WHEN sign THEN output is url-safe and keeps padding`() {
        // Act
        val actual = signer.sign(SECRET, "1700000000GET/order")

        // Assert
        assertThat(actual).doesNotContain("+")
        assertThat(actual).doesNotContain("/")
        assertThat(actual).endsWith("=")
    }

    @Test
    fun `GIVEN url-safe secret WHEN sign THEN decodes the dash-underscore alphabet and matches the vector`() {
        // Act
        val actual = signer.sign("-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_", "1700000000GET/order")

        // Assert
        assertThat(actual).isEqualTo("lMBf4WgRKT3_qLflD-0AftEGiUDIrZOCdc-ZwWmQUx4=")
    }

    private companion object {
        const val SECRET = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8="
    }
}