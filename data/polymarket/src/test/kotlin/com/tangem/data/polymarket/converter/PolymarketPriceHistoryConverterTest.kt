package com.tangem.data.polymarket.converter

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.domain.polymarket.model.PolymarketPricePoint
import com.tangem.grow.datasource.polymarket.clob.models.PolymarketPriceHistoryResponse
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class PolymarketPriceHistoryConverterTest {

    private val adapter = Moshi.Builder().build().adapter(PolymarketPriceHistoryResponse::class.java)

    @Test
    fun `GIVEN unsorted wire points WHEN converted THEN UTC milliseconds and exact probabilities are sorted`() {
        // Arrange
        val response = requireNotNull(adapter.fromJson("""
            {"history":[
              {"t":1787000300,"p":0.1234567890123456789},
              {"t":1787000100,"p":"0.25"},
              {"t":1787000200,"p":0.75}
            ]}
        """.trimIndent()))

        // Act
        val actual = PolymarketPriceHistoryConverter.convert(response)

        // Assert
        assertThat(actual).containsExactly(
            PolymarketPricePoint(1_787_000_100_000, BigDecimal("0.25")),
            PolymarketPricePoint(1_787_000_200_000, BigDecimal("0.75")),
            PolymarketPricePoint(1_787_000_300_000, BigDecimal("0.1234567890123456789")),
        ).inOrder()
    }

    @Test
    fun `GIVEN duplicate timestamps WHEN converted THEN the last valid probability wins`() {
        // Arrange
        val response = PolymarketPriceHistoryResponse(
            listOf(point(price = "0.25"), point(price = "0.75"), point(price = "2")),
        )

        // Act
        val actual = PolymarketPriceHistoryConverter.convert(response)

        // Assert
        assertThat(actual).containsExactly(PolymarketPricePoint(1_000, BigDecimal("0.75")))
    }

    @Test
    fun `GIVEN invalid points and valid boundaries WHEN converted THEN only real zero and one survive`() {
        // Arrange
        val response = requireNotNull(adapter.fromJson("""
            {"history":[
              {"t":1,"p":0}, {"t":2,"p":1}, {"t":3,"p":-0.01}, {"t":4,"p":1.01},
              {"t":5,"p":"NaN"}, {"t":6,"p":"Infinity"}, {"t":7,"p":null}, {"t":8},
              {"p":0.5}, {"t":-1,"p":0.5}, {"t":9223372036854775807,"p":0.5}
            ]}
        """.trimIndent()))

        // Act
        val actual = PolymarketPriceHistoryConverter.convert(response)

        // Assert
        assertThat(actual).containsExactly(
            PolymarketPricePoint(1_000, BigDecimal.ZERO),
            PolymarketPricePoint(2_000, BigDecimal.ONE),
        ).inOrder()
    }

    @Test
    fun `GIVEN empty history WHEN converted THEN no synthetic points are created`() {
        // Arrange
        val response = PolymarketPriceHistoryResponse(emptyList())

        // Act
        val actual = PolymarketPriceHistoryConverter.convert(response)

        // Assert
        assertThat(actual).isEmpty()
    }

    private fun point(seconds: Long = 1, price: String = "0.5") = PolymarketPriceHistoryResponse.Point(seconds, price)
}