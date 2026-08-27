package com.tangem.features.polymarket.impl.main.model.converter

import com.google.common.truth.Truth
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.polymarket.model.PolymarketDisplayMode
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketMarket
import com.tangem.domain.polymarket.model.PolymarketOutcome
import com.tangem.domain.polymarket.model.PolymarketStatus
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketEventUMConverterTest {

    private val clickedEvents = mutableListOf<String>()
    private val clickedOutcomes = mutableListOf<Triple<String, String, String>>()

    private val converter = PolymarketEventUMConverter(
        onEventClick = { clickedEvents += it },
        onOutcomeClick = { eventId, marketId, assetId -> clickedOutcomes += Triple(eventId, marketId, assetId) },
    )

    // The class is PER_CLASS, so the recorded clicks would leak between tests.
    @BeforeEach
    fun resetRecordedClicks() {
        clickedEvents.clear()
        clickedOutcomes.clear()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RowTitle {

        @ParameterizedTest
        @ProvideTestModels
        fun convertRowTitle(model: RowTitleModel) {
            // Arrange
            val event = createEvent(
                displayMode = model.displayMode,
                markets = listOf(createMarket(groupItemTitle = model.groupItemTitle)),
            )

            // Act
            val actual = converter.convert(event)

            // Assert
            Truth.assertThat(actual.rows.single().title).isEqualTo(model.expected)
        }

        @Test
        fun `GIVEN plain event with several markets WHEN convert THEN every row names its own question`() {
            // Arrange
            val event = createEvent(
                displayMode = PolymarketDisplayMode.PLAIN_MARKETS,
                markets = listOf(
                    createMarket(id = "market-1", title = "Will Haaland join Real Madrid?"),
                    createMarket(id = "market-2", title = "Will Vinicius stay at Real Madrid?"),
                ),
            )

            // Act
            val actual = converter.convert(event)

            // Assert
            Truth.assertThat(actual.rows.map { it.title })
                .containsExactly(
                    stringReference("Will Haaland join Real Madrid?"),
                    stringReference("Will Vinicius stay at Real Madrid?"),
                )
                .inOrder()
        }

        private fun provideTestModels(): List<RowTitleModel> = listOf(
            // A grouped event competes markets against each other, so the group item title labels the row.
            RowTitleModel(
                displayMode = PolymarketDisplayMode.GROUPED_OUTCOMES,
                groupItemTitle = "France",
                expected = stringReference("France"),
            ),
            // A grouped market without a group item title falls back to its question.
            RowTitleModel(
                displayMode = PolymarketDisplayMode.GROUPED_OUTCOMES,
                groupItemTitle = null,
                expected = stringReference("Market question"),
            ),
            // A plain event asks a single question, already shown as the card title.
            RowTitleModel(
                displayMode = PolymarketDisplayMode.PLAIN_MARKETS,
                groupItemTitle = null,
                expected = resourceReference(R.string.prediction_event_probability),
            ),
            // Even when a plain market reports a group item title, it is not what labels the row.
            RowTitleModel(
                displayMode = PolymarketDisplayMode.PLAIN_MARKETS,
                groupItemTitle = "France",
                expected = resourceReference(R.string.prediction_event_probability),
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class HiddenMarketsCount {

        @ParameterizedTest
        @ProvideTestModels
        fun convertHiddenMarketsCount(model: HiddenCountModel) {
            // Arrange
            val event = createEvent(
                totalMarketsCount = model.totalMarketsCount,
                markets = List(model.shownMarketsCount) { createMarket(id = "market-$it") },
            )

            // Act
            val actual = converter.convert(event)

            // Assert
            Truth.assertThat(actual.hiddenMarketsCount).isEqualTo(model.expected)
        }

        private fun provideTestModels(): List<HiddenCountModel> = listOf(
            HiddenCountModel(totalMarketsCount = 6, shownMarketsCount = 2, expected = 4),
            HiddenCountModel(totalMarketsCount = 1, shownMarketsCount = 1, expected = 0),
            // The feed must never claim a negative number of hidden markets.
            HiddenCountModel(totalMarketsCount = 0, shownMarketsCount = 2, expected = 0),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Formatting {

        // The formatted volume itself is not covered here: `compact()` relies on android.icu.text.CompactDecimalFormat,
        // which is unavailable to JVM unit tests.

        @Test
        fun `GIVEN no volume WHEN convert THEN volume is null`() {
            // Act
            val actual = converter.convert(createEvent(volume = null))

            // Assert
            Truth.assertThat(actual.volume).isNull()
        }

        @Test
        fun `GIVEN probability WHEN convert THEN it is a whole percent of the first outcome`() {
            // Arrange
            val event = createEvent(
                markets = listOf(
                    createMarket(
                        outcomes = listOf(
                            createOutcome(assetId = "yes", title = "Yes", probability = BigDecimal("0.8")),
                            createOutcome(assetId = "no", title = "No", probability = BigDecimal("0.2")),
                        ),
                    ),
                ),
            )

            // Act
            val actual = converter.convert(event)

            // Assert
            Truth.assertThat(actual.rows.single().probability).isEqualTo(stringReference("80%"))
        }

        @Test
        fun `GIVEN no probability WHEN convert THEN probability is null`() {
            // Arrange
            val event = createEvent(
                markets = listOf(createMarket(outcomes = listOf(createOutcome(probability = null)))),
            )

            // Act
            val actual = converter.convert(event)

            // Assert
            Truth.assertThat(actual.rows.single().probability).isNull()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Outcomes {

        @Test
        fun `GIVEN backend labels WHEN convert THEN they are used as is`() {
            // Arrange
            val event = createEvent(
                markets = listOf(
                    createMarket(
                        outcomes = listOf(
                            createOutcome(assetId = "a", title = "Neim"),
                            createOutcome(assetId = "b", title = "Todo"),
                        ),
                    ),
                ),
            )

            // Act
            val actual = converter.convert(event)

            // Assert
            Truth.assertThat(actual.rows.single().outcomes.map { it.title })
                .containsExactly(stringReference("Neim"), stringReference("Todo"))
                .inOrder()
        }

        @Test
        fun `GIVEN converted event WHEN outcome clicked THEN event market and asset ids are reported`() {
            // Arrange
            val event = createEvent(
                markets = listOf(
                    createMarket(id = "market-1", outcomes = listOf(createOutcome(assetId = "asset-1"))),
                ),
            )

            // Act
            converter.convert(event).rows.single().outcomes.single().onClick()

            // Assert
            Truth.assertThat(clickedOutcomes).containsExactly(Triple("event-1", "market-1", "asset-1"))
        }

        @Test
        fun `GIVEN converted event WHEN card clicked THEN event id is reported`() {
            // Act
            converter.convert(createEvent()).onClick()

            // Assert
            Truth.assertThat(clickedEvents).containsExactly("event-1")
        }
    }

    internal data class RowTitleModel(
        val displayMode: PolymarketDisplayMode,
        val groupItemTitle: String?,
        val expected: TextReference,
    )

    internal data class HiddenCountModel(
        val totalMarketsCount: Int,
        val shownMarketsCount: Int,
        val expected: Int,
    )

    private fun createEvent(
        displayMode: PolymarketDisplayMode = PolymarketDisplayMode.GROUPED_OUTCOMES,
        volume: BigDecimal? = null,
        totalMarketsCount: Int = 1,
        markets: List<PolymarketMarket> = listOf(createMarket()),
    ): PolymarketEvent = PolymarketEvent(
        id = "event-1",
        slug = "event-slug",
        title = "Event title",
        description = "Event description",
        rulesUrl = "https://polymarket.com/rules",
        iconUrl = null,
        imageUrl = null,
        status = PolymarketStatus.ACTIVE,
        startDate = null,
        endDate = null,
        volume = volume,
        volume24h = null,
        liquidity = null,
        totalMarketsCount = totalMarketsCount,
        isNegRisk = displayMode == PolymarketDisplayMode.GROUPED_OUTCOMES,
        displayMode = displayMode,
        markets = markets,
    )

    private fun createMarket(
        id: String = "market-1",
        title: String = "Market question",
        groupItemTitle: String? = null,
        outcomes: List<PolymarketOutcome> = listOf(createOutcome()),
    ): PolymarketMarket = PolymarketMarket(
        id = id,
        eventId = "event-1",
        title = title,
        slug = "market-slug",
        description = "Market description",
        groupItemTitle = groupItemTitle,
        iconUrl = null,
        imageUrl = null,
        status = PolymarketStatus.ACTIVE,
        isNegRisk = false,
        startDate = null,
        endDate = null,
        startDateIso = null,
        endDateIso = null,
        volume = null,
        volume24h = null,
        liquidity = null,
        orderIndex = 0,
        outcomes = outcomes,
    )

    private fun createOutcome(
        assetId: String = "asset-1",
        title: String = "Yes",
        probability: BigDecimal? = null,
    ): PolymarketOutcome = PolymarketOutcome(
        assetId = assetId,
        title = title,
        probability = probability,
    )
}