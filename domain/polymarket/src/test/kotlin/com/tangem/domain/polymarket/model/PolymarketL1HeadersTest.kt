package com.tangem.domain.polymarket.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class PolymarketL1HeadersTest {

    @Test
    fun `GIVEN headers WHEN toString THEN signature and nonce are redacted`() {
        // Arrange
        val headers = PolymarketL1Headers(
            address = "0xabc",
            signature = "0xdeadbeefsignature",
            timestamp = "1700000000",
            nonce = "42",
        )

        // Act
        val actual = headers.toString()

        // Assert
        assertThat(actual).doesNotContain("0xdeadbeefsignature")
        assertThat(actual).doesNotContain("42")
        assertThat(actual).contains("0xabc")
        assertThat(actual).contains("1700000000")
    }

    @Test
    fun `GIVEN headers WHEN toMap THEN all values are preserved under the POLY headers`() {
        // Arrange
        val headers = PolymarketL1Headers(
            address = "0xabc",
            signature = "0xsig",
            timestamp = "1700000000",
            nonce = "42",
        )

        // Act
        val actual = headers.toMap()

        // Assert
        assertThat(actual).containsExactly(
            "POLY_ADDRESS", "0xabc",
            "POLY_SIGNATURE", "0xsig",
            "POLY_TIMESTAMP", "1700000000",
            "POLY_NONCE", "42",
        )
    }
}