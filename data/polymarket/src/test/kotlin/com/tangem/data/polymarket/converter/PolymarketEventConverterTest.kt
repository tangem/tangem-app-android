package com.tangem.data.polymarket.converter

import com.google.common.truth.Truth
import com.tangem.datasource.api.polymarket.models.PolymarketEventDto
import com.tangem.datasource.api.polymarket.models.PolymarketMarketDto
import com.tangem.datasource.api.polymarket.models.PolymarketOutcomeDto
import com.tangem.domain.polymarket.model.PolymarketDisplayMode
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketMarket
import com.tangem.domain.polymarket.model.PolymarketOutcome
import com.tangem.domain.polymarket.model.PolymarketStatus
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketEventConverterTest {

    private val converter = PolymarketEventConverter()

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Convert {

        @Test
        fun `GIVEN full event dto WHEN convert THEN every field is mapped`() {
            // Arrange
            val dto = createEventDto(
                markets = listOf(
                    createMarketDto(
                        outcomes = listOf(
                            PolymarketOutcomeDto(assetId = "asset-yes", label = "Yes", probability = 0.18),
                        ),
                    ),
                ),
            )

            // Act
            val actual = converter.convert(value = dto)

            // Assert
            val expected = PolymarketEvent(
                id = "event-id",
                slug = "event-slug",
                title = "Event title",
                description = "Event description",
                rulesUrl = "https://polymarket.com/rules",
                iconUrl = "https://cdn.tangem.com/event-icon.png",
                imageUrl = "https://cdn.tangem.com/event-image.png",
                status = PolymarketStatus.ACTIVE,
                startDate = "2026-06-11T16:00:00Z",
                endDate = "2026-07-19T20:00:00Z",
                volume = BigDecimal("1234.5"),
                volume24h = BigDecimal("67.8"),
                liquidity = BigDecimal("910.25"),
                totalMarketsCount = 24,
                isNegRisk = true,
                displayMode = PolymarketDisplayMode.GROUPED_OUTCOMES,
                markets = listOf(
                    PolymarketMarket(
                        id = "market-id",
                        eventId = "event-id",
                        title = "Market question",
                        slug = "market-slug",
                        description = "Market description",
                        groupItemTitle = "Argentina",
                        iconUrl = "https://cdn.tangem.com/market-icon.png",
                        imageUrl = "https://cdn.tangem.com/market-image.png",
                        status = PolymarketStatus.ACTIVE,
                        isNegRisk = true,
                        startDate = "2026-06-11T16:00:00Z",
                        endDate = "2026-07-19T20:00:00Z",
                        startDateIso = "2026-06-11",
                        endDateIso = "2026-07-19",
                        volume = BigDecimal("12.5"),
                        volume24h = BigDecimal("3.75"),
                        liquidity = BigDecimal("8.5"),
                        orderIndex = 1,
                        outcomes = listOf(
                            PolymarketOutcome(assetId = "asset-yes", title = "Yes", probability = BigDecimal("0.18")),
                        ),
                    ),
                ),
            )
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `GIVEN nullable fields are absent WHEN convert THEN they are mapped to null`() {
            // Arrange
            val dto = createEventDto(
                icon = null,
                image = null,
                startDate = null,
                endDate = null,
                volume = null,
                volume24hr = null,
                liquidity = null,
                markets = emptyList(),
            )

            // Act
            val actual = converter.convert(value = dto)

            // Assert
            Truth.assertThat(actual.iconUrl).isNull()
            Truth.assertThat(actual.imageUrl).isNull()
            Truth.assertThat(actual.startDate).isNull()
            Truth.assertThat(actual.endDate).isNull()
            Truth.assertThat(actual.volume).isNull()
            Truth.assertThat(actual.volume24h).isNull()
            Truth.assertThat(actual.liquidity).isNull()
            Truth.assertThat(actual.markets).isEmpty()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ConvertStatus {

        @ParameterizedTest
        @ProvideTestModels
        fun convertStatus(model: StatusModel) {
            // Act
            val actual = converter.convert(value = createEventDto(status = model.status))

            // Assert
            Truth.assertThat(actual.status).isEqualTo(model.expected)
        }

        private fun provideTestModels(): List<StatusModel> {
            return listOf(
                StatusModel(status = "active", expected = PolymarketStatus.ACTIVE),
                StatusModel(status = "closed", expected = PolymarketStatus.CLOSED),
                StatusModel(status = "archived", expected = PolymarketStatus.ARCHIVED),
                StatusModel(status = "resolving", expected = PolymarketStatus.UNKNOWN),
                StatusModel(status = "", expected = PolymarketStatus.UNKNOWN),
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ConvertDisplayMode {

        @ParameterizedTest
        @ProvideTestModels
        fun convertDisplayMode(model: DisplayModeModel) {
            // Act
            val dto = createEventDto(displayMode = model.displayMode, isNegRisk = model.isNegRisk)
            val actual = converter.convert(value = dto)

            // Assert
            Truth.assertThat(actual.displayMode).isEqualTo(model.expected)
        }

        private fun provideTestModels(): List<DisplayModeModel> {
            return listOf(
                DisplayModeModel(
                    displayMode = "grouped_outcomes",
                    isNegRisk = true,
                    expected = PolymarketDisplayMode.GROUPED_OUTCOMES,
                ),
                DisplayModeModel(
                    displayMode = "plain_markets",
                    isNegRisk = false,
                    expected = PolymarketDisplayMode.PLAIN_MARKETS,
                ),
                // The BFF owns the display mode, so the reported one wins over the isNegRisk-derived default.
                DisplayModeModel(
                    displayMode = "plain_markets",
                    isNegRisk = true,
                    expected = PolymarketDisplayMode.PLAIN_MARKETS,
                ),
                // An unrecognized mode falls back to the BFF's own rule: grouped when isNegRisk is set.
                DisplayModeModel(
                    displayMode = "carousel",
                    isNegRisk = true,
                    expected = PolymarketDisplayMode.GROUPED_OUTCOMES,
                ),
                DisplayModeModel(
                    displayMode = "carousel",
                    isNegRisk = false,
                    expected = PolymarketDisplayMode.PLAIN_MARKETS,
                ),
            )
        }
    }

    internal data class StatusModel(val status: String, val expected: PolymarketStatus)

    internal data class DisplayModeModel(
        val displayMode: String,
        val isNegRisk: Boolean,
        val expected: PolymarketDisplayMode,
    )

    private fun createEventDto(
        status: String = "active",
        displayMode: String = "grouped_outcomes",
        isNegRisk: Boolean = true,
        icon: String? = "https://cdn.tangem.com/event-icon.png",
        image: String? = "https://cdn.tangem.com/event-image.png",
        startDate: String? = "2026-06-11T16:00:00Z",
        endDate: String? = "2026-07-19T20:00:00Z",
        volume: Double? = 1234.5,
        volume24hr: Double? = 67.8,
        liquidity: Double? = 910.25,
        markets: List<PolymarketMarketDto> = emptyList(),
    ): PolymarketEventDto = PolymarketEventDto(
        eventId = "event-id",
        slug = "event-slug",
        title = "Event title",
        description = "Event description",
        polymarketRulesUrl = "https://polymarket.com/rules",
        icon = icon,
        image = image,
        status = status,
        startDate = startDate,
        endDate = endDate,
        volume = volume,
        volume24hr = volume24hr,
        liquidity = liquidity,
        totalMarketsCount = 24,
        isNegRisk = isNegRisk,
        displayMode = displayMode,
        markets = markets,
    )

    private fun createMarketDto(outcomes: List<PolymarketOutcomeDto> = emptyList()): PolymarketMarketDto =
        PolymarketMarketDto(
            id = "market-id",
            eventId = "event-id",
            question = "Market question",
            slug = "market-slug",
            description = "Market description",
            groupItemTitle = "Argentina",
            icon = "https://cdn.tangem.com/market-icon.png",
            image = "https://cdn.tangem.com/market-image.png",
            status = "active",
            isNegRisk = true,
            startDate = "2026-06-11T16:00:00Z",
            endDate = "2026-07-19T20:00:00Z",
            startDateIso = "2026-06-11",
            endDateIso = "2026-07-19",
            volume = 12.5,
            volume24hr = 3.75,
            liquidity = 8.5,
            orderIndex = 1,
            outcomes = outcomes,
        )
}