package com.tangem.data.polymarket.quote

import com.google.common.truth.Truth.assertThat
import com.tangem.grow.datasource.polymarket.socket.PolymarketMarketEventDto
import com.tangem.test.core.ProvideTestModels
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketQuoteConverterTest {

    @Test
    fun `GIVEN unsorted levels and zero sizes WHEN converted THEN the best executable prices retain precision`() {
        // Arrange
        val book = PolymarketMarketEventDto.Book(
            "a",
            bids = listOf(level("0.42"), level("0.6", "0"), level("0.51")),
            asks = listOf(level("0.7"), level("0.611111111111111111"), level("0.52", "0")),
            timestamp = "100",
        )

        // Act
        val actual = PolymarketQuoteConverter.convert(book)

        // Assert
        assertThat(actual).containsExactly(
            PolymarketQuoteUpdate("a", 100, side("0.51"), side("0.611111111111111111")),
        )
    }

    @Test
    fun `GIVEN partial changes and empty side sentinels WHEN converted THEN absence differs from clearing`() {
        // Arrange
        val event = PolymarketMarketEventDto.PriceChange(
            listOf(
                PolymarketMarketEventDto.Change("a", bestAsk = "0.6"),
                PolymarketMarketEventDto.Change("b", "0", "1"),
                PolymarketMarketEventDto.Change("c"),
            ),
            "200",
        )

        // Act
        val actual = PolymarketQuoteConverter.convert(event)

        // Assert
        assertThat(actual).containsExactly(
            PolymarketQuoteUpdate("a", 200, ask = side("0.6")),
            PolymarketQuoteUpdate("b", 200, side(null), side(null)),
        ).inOrder()
    }

    @Test
    fun `GIVEN an empty snapshot WHEN converted THEN both sides are explicitly cleared`() {
        // Arrange
        val event = PolymarketMarketEventDto.Book("a", emptyList(), emptyList(), "300")

        // Act
        val actual = PolymarketQuoteConverter.convert(event)

        // Assert
        assertThat(actual).containsExactly(PolymarketQuoteUpdate("a", 300, side(null), side(null)))
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN malformed market data WHEN converted THEN no invented price escapes`(event: PolymarketMarketEventDto) {
        // Arrange
        val input = event

        // Act
        val actual = PolymarketQuoteConverter.convert(input)

        // Assert
        assertThat(actual).isEmpty()
    }

    private fun provideTestModels(): List<PolymarketMarketEventDto> = listOf(
        PolymarketMarketEventDto.BestBidAsk("a", "NaN", "0.5", "100"),
        PolymarketMarketEventDto.BestBidAsk("a", "0.4", "1.2", "100"),
        PolymarketMarketEventDto.BestBidAsk("a", "-0.1", "0.5", "100"),
        PolymarketMarketEventDto.BestBidAsk("a", "0.4", "0.5", "invalid"),
        PolymarketMarketEventDto.LastTradePrice("a", "0.5", "-1"),
        PolymarketMarketEventDto.LastTradePrice("a", "unknown", "100"),
        PolymarketMarketEventDto.Book("a", listOf(level("bad")), emptyList(), "100"),
        PolymarketMarketEventDto.Book("a", emptyList(), listOf(level("0.5", "-2")), "100"),
        PolymarketMarketEventDto.Unknown("new", JsonObject(emptyMap())),
    )

    private fun level(price: String, size: String = "10") = PolymarketMarketEventDto.Level(price, size)

    private fun side(price: String?) = PolymarketQuoteUpdate.Side(price?.let(::BigDecimal))
}