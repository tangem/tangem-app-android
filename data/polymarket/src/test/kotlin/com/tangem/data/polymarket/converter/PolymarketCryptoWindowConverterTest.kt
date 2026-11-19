package com.tangem.data.polymarket.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketCryptoNextWindow
import com.tangem.domain.polymarket.model.PolymarketCryptoPriceFeed
import com.tangem.domain.polymarket.model.PolymarketCryptoWindow
import com.tangem.grow.datasource.polymarket.models.PolymarketCryptoNextEventDto
import com.tangem.grow.datasource.polymarket.models.PolymarketCryptoPriceFeedDto
import com.tangem.grow.datasource.polymarket.models.PolymarketCryptoPriceFeedFilterDto
import com.tangem.grow.datasource.polymarket.models.PolymarketCryptoUpDownDto
import com.tangem.grow.datasource.polymarket.models.PolymarketEventDto
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.time.Instant

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketCryptoWindowConverterTest {

    private val converter = PolymarketCryptoWindowConverter

    @Test
    fun `GIVEN event details block WHEN convert THEN the whole window is read`() {
        // Arrange
        val dto = createEventDto(
            block = createBlock(
                finalPrice = 124.5,
                nextEvents = listOf(
                    NEXT_EVENT,
                    PolymarketCryptoNextEventDto(
                        id = "next-2",
                        windowStart = "2026-01-01T10:10:00Z",
                        windowEnd = "2026-01-01T10:15:00Z",
                    ),
                ),
                liveEventId = "next-1",
            ),
        )

        // Act
        val actual = converter.convert(dto)

        // Assert
        val expected = PolymarketCryptoWindow(
            start = Instant.parse("2026-01-01T10:00:00Z"),
            end = Instant.parse("2026-01-01T10:05:00Z"),
            priceFeed = PolymarketCryptoPriceFeed(
                channel = "price.crypto.twap",
                symbol = "coinusd",
                provider = null,
                windowSeconds = 60,
            ),
            priceToBeat = BigDecimal("123.456"),
            finalPrice = BigDecimal("124.5"),
            nextWindows = listOf(
                NEXT_WINDOW,
                PolymarketCryptoNextWindow(
                    eventId = "next-2",
                    start = Instant.parse("2026-01-01T10:10:00Z"),
                    end = Instant.parse("2026-01-01T10:15:00Z"),
                ),
            ),
            liveEventId = "next-1",
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN feed block without prices and neighbours WHEN convert THEN absent values stay absent`() {
        // Arrange
        val dto = createEventDto(
            block = createBlock(priceToBeat = null, finalPrice = null, nextEvents = null, liveEventId = null),
        )

        // Act
        val actual = converter.convert(dto)

        // Assert
        assertThat(actual?.priceToBeat).isNull()
        assertThat(actual?.finalPrice).isNull()
        assertThat(actual?.nextWindows).isEmpty()
        assertThat(actual?.liveEventId).isNull()
    }

    @Test
    fun `GIVEN zero price WHEN convert THEN it is a price and not an absent one`() {
        // Arrange
        val dto = createEventDto(block = createBlock(priceToBeat = 0.0))

        // Act
        val actual = converter.convert(dto)

        // Assert
        assertThat(actual?.priceToBeat).isEqualToIgnoringScale(BigDecimal.ZERO)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class NoWindow {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN no valid window WHEN convert THEN the event stays ordinary`(model: NoWindowModel) {
            // Act
            val actual = converter.convert(createEventDto(endDate = model.endDate, block = model.block))

            // Assert
            assertThat(actual).isNull()
        }

        private fun provideTestModels() = listOf(
            NoWindowModel(case = "no block", block = null),
            NoWindowModel(case = "no window start", block = createBlock(windowStart = null)),
            NoWindowModel(case = "window start is not a date", block = createBlock(windowStart = "10:00")),
            NoWindowModel(case = "no end date", endDate = null),
            NoWindowModel(case = "end date is not a date", endDate = "tomorrow"),
            NoWindowModel(case = "end at the start", endDate = "2026-01-01T10:00:00Z"),
            NoWindowModel(case = "end before the start", endDate = "2026-01-01T09:55:00Z"),
        )
    }

    data class NoWindowModel(
        val case: String,
        val endDate: String? = "2026-01-01T10:05:00Z",
        val block: PolymarketCryptoUpDownDto? = createBlock(),
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PriceFeed {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN price feed WHEN convert THEN it is read or left absent`(model: PriceFeedModel) {
            // Act
            val actual = converter.convert(createEventDto(block = createBlock(priceFeed = model.feed)))

            // Assert
            assertThat(actual).isNotNull()
            assertThat(actual?.priceFeed).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            PriceFeedModel(
                case = "spot channel pinned to a provider",
                feed = createFeed(channel = "price.crypto", provider = "binance", windowSeconds = null),
                expected = PolymarketCryptoPriceFeed(
                    channel = "price.crypto",
                    symbol = "coinusd",
                    provider = "binance",
                    windowSeconds = null,
                ),
            ),
            PriceFeedModel(
                case = "blank provider",
                feed = createFeed(provider = " "),
                expected = PolymarketCryptoPriceFeed(
                    channel = "price.crypto.twap",
                    symbol = "coinusd",
                    provider = null,
                    windowSeconds = 60,
                ),
            ),
            PriceFeedModel(case = "no feed", feed = null, expected = null),
            PriceFeedModel(case = "no channel", feed = createFeed(channel = null), expected = null),
            PriceFeedModel(case = "blank channel", feed = createFeed(channel = " "), expected = null),
            PriceFeedModel(
                case = "no filter",
                feed = PolymarketCryptoPriceFeedDto(channel = "price.crypto", filter = null),
                expected = null,
            ),
            PriceFeedModel(case = "no symbol", feed = createFeed(symbol = null), expected = null),
            PriceFeedModel(case = "blank symbol", feed = createFeed(symbol = " "), expected = null),
        )
    }

    data class PriceFeedModel(
        val case: String,
        val feed: PolymarketCryptoPriceFeedDto?,
        val expected: PolymarketCryptoPriceFeed?,
    )

    @Test
    fun `GIVEN next events with broken entries WHEN convert THEN only valid windows are kept`() {
        // Arrange
        val dto = createEventDto(
            block = createBlock(
                nextEvents = listOf(
                    NEXT_EVENT.copy(id = null),
                    NEXT_EVENT.copy(id = " "),
                    NEXT_EVENT.copy(id = "no-start", windowStart = null),
                    NEXT_EVENT.copy(id = "bad-start", windowStart = "soon"),
                    NEXT_EVENT.copy(id = "no-end", windowEnd = null),
                    NEXT_EVENT.copy(id = "bad-end", windowEnd = "later"),
                    NEXT_EVENT.copy(id = "empty", windowEnd = "2026-01-01T10:05:00Z"),
                    NEXT_EVENT,
                ),
            ),
        )

        // Act
        val actual = converter.convert(dto)

        // Assert
        assertThat(actual?.nextWindows).containsExactly(NEXT_WINDOW)
    }

    @Test
    fun `GIVEN blank live event id WHEN convert THEN it is absent`() {
        // Arrange
        val dto = createEventDto(block = createBlock(liveEventId = " "))

        // Act
        val actual = converter.convert(dto)

        // Assert
        assertThat(actual).isNotNull()
        assertThat(actual?.liveEventId).isNull()
    }

    private fun createEventDto(
        endDate: String? = "2026-01-01T10:05:00Z",
        block: PolymarketCryptoUpDownDto? = createBlock(),
    ): PolymarketEventDto = PolymarketEventDto(
        eventId = "event-id",
        slug = "coin-updown-5m",
        title = "Coin Up or Down",
        description = "Event description",
        polymarketRulesUrl = "https://polymarket.com/rules",
        icon = null,
        image = null,
        status = "active",
        startDate = "2025-12-31T10:00:00Z",
        endDate = endDate,
        volume = null,
        volume24hr = null,
        liquidity = null,
        totalMarketsCount = 1,
        isNegRisk = false,
        isLive = false,
        displayMode = "plain_markets",
        markets = emptyList(),
        eventType = "crypto",
        cryptoUpDown = block,
    )

    private companion object {

        val NEXT_EVENT = PolymarketCryptoNextEventDto(
            id = "next-1",
            windowStart = "2026-01-01T10:05:00Z",
            windowEnd = "2026-01-01T10:10:00Z",
        )

        val NEXT_WINDOW = PolymarketCryptoNextWindow(
            eventId = "next-1",
            start = Instant.parse("2026-01-01T10:05:00Z"),
            end = Instant.parse("2026-01-01T10:10:00Z"),
        )

        fun createFeed(
            channel: String? = "price.crypto.twap",
            symbol: String? = "coinusd",
            provider: String? = null,
            windowSeconds: Int? = 60,
        ) = PolymarketCryptoPriceFeedDto(
            channel = channel,
            filter = PolymarketCryptoPriceFeedFilterDto(
                symbol = symbol,
                provider = provider,
                windowSeconds = windowSeconds,
            ),
        )

        fun createBlock(
            windowStart: String? = "2026-01-01T10:00:00Z",
            priceFeed: PolymarketCryptoPriceFeedDto? = createFeed(),
            priceToBeat: Double? = 123.456,
            finalPrice: Double? = null,
            nextEvents: List<PolymarketCryptoNextEventDto>? = null,
            liveEventId: String? = "event-id",
        ) = PolymarketCryptoUpDownDto(
            windowStart = windowStart,
            priceFeed = priceFeed,
            priceToBeat = priceToBeat,
            finalPrice = finalPrice,
            nextEvents = nextEvents,
            liveEventId = liveEventId,
        )
    }
}