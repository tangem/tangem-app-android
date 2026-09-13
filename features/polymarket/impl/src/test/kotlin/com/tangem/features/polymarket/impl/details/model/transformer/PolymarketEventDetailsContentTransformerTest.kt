package com.tangem.features.polymarket.impl.details.model.transformer

import com.google.common.truth.Truth
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.polymarket.model.PolymarketDisplayMode
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketMarket
import com.tangem.domain.polymarket.model.PolymarketOutcome
import com.tangem.domain.polymarket.model.PolymarketStatus
import com.tangem.features.polymarket.impl.details.ui.state.PolymarketEventDetailsUM
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketEventDetailsContentTransformerTest {

    private val sharedSlugs = mutableListOf<String>()
    private val clickedOutcomes = mutableListOf<Pair<String, String>>()
    private var closedMarketsToggles = 0
    private var readMoreClicks = 0

    // The class is PER_CLASS, so the recorded callbacks would leak between tests.
    @BeforeEach
    fun resetRecordedCallbacks() {
        sharedSlugs.clear()
        clickedOutcomes.clear()
        closedMarketsToggles = 0
        readMoreClicks = 0
    }

    private fun transform(
        event: PolymarketEvent,
        isClosedMarketsExpanded: Boolean = false,
        isDescriptionExpanded: Boolean = false,
    ) = PolymarketEventDetailsContentTransformer(
        event = event,
        isClosedMarketsExpanded = isClosedMarketsExpanded,
        isDescriptionExpanded = isDescriptionExpanded,
        onShareClick = { sharedSlugs += it },
        onOutcomeClick = { marketId, assetId -> clickedOutcomes += marketId to assetId },
        onClosedMarketsClick = { closedMarketsToggles++ },
        onReadMoreClick = { readMoreClicks++ },
    ).transform(prevState = PolymarketEventDetailsUM.Loading) as PolymarketEventDetailsUM.Content

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class MarketsSplit {

        @Test
        fun `GIVEN mixed statuses WHEN transform THEN active and closed split and archived dropped`() {
            // Arrange
            val event = createEvent(
                markets = listOf(
                    createMarket(id = "active", status = PolymarketStatus.ACTIVE),
                    createMarket(id = "closed", status = PolymarketStatus.CLOSED),
                    createMarket(id = "archived", status = PolymarketStatus.ARCHIVED),
                    createMarket(id = "unknown", status = PolymarketStatus.UNKNOWN),
                ),
            )

            // Act
            val actual = transform(event)

            // Assert
            Truth.assertThat(actual.activeMarkets.map { it.id }).containsExactly("active", "unknown").inOrder()
            Truth.assertThat(actual.closedMarkets.map { it.id }).containsExactly("closed")
        }

        @Test
        fun `GIVEN unsorted order indexes WHEN transform THEN markets follow orderIndex`() {
            // Arrange
            val event = createEvent(
                markets = listOf(
                    createMarket(id = "third", orderIndex = 2),
                    createMarket(id = "first", orderIndex = 0),
                    createMarket(id = "second", orderIndex = 1),
                ),
            )

            // Act
            val actual = transform(event)

            // Assert
            Truth.assertThat(actual.activeMarkets.map { it.id }).containsExactly("first", "second", "third").inOrder()
        }

        @Test
        fun `GIVEN closed market WHEN transform THEN it carries no outcome buttons`() {
            // Arrange
            val event = createEvent(
                markets = listOf(
                    createMarket(
                        id = "closed",
                        status = PolymarketStatus.CLOSED,
                        outcomes = listOf(createOutcome(assetId = "yes"), createOutcome(assetId = "no")),
                    ),
                ),
            )

            // Act
            val actual = transform(event)

            // Assert
            Truth.assertThat(actual.closedMarkets.single().outcomes).isEmpty()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class MarketTitle {

        @Test
        fun `GIVEN grouped event WHEN transform THEN group item title labels the card falling back to question`() {
            // Arrange
            val event = createEvent(
                displayMode = PolymarketDisplayMode.GROUPED_OUTCOMES,
                markets = listOf(
                    createMarket(id = "labelled", groupItemTitle = "France"),
                    createMarket(id = "unlabelled", groupItemTitle = null, title = "Will France win?"),
                ),
            )

            // Act
            val actual = transform(event)

            // Assert
            Truth.assertThat(actual.activeMarkets.map { it.title })
                .containsExactly(stringReference("France"), stringReference("Will France win?"))
                .inOrder()
        }

        @Test
        fun `GIVEN plain event WHEN transform THEN each card names its own question`() {
            // Arrange
            val event = createEvent(
                displayMode = PolymarketDisplayMode.PLAIN_MARKETS,
                markets = listOf(createMarket(title = "Will ETH reach 5000?", groupItemTitle = "Ignored")),
            )

            // Act
            val actual = transform(event)

            // Assert
            Truth.assertThat(actual.activeMarkets.single().title).isEqualTo(stringReference("Will ETH reach 5000?"))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OutcomeLabels {

        @ParameterizedTest
        @ProvideTestModels
        fun transformOutcomeLabel(model: OutcomeLabelModel) {
            // Arrange
            val event = createEvent(
                markets = listOf(
                    createMarket(outcomes = listOf(createOutcome(title = model.title, probability = model.probability))),
                ),
            )

            // Act
            val actual = transform(event)

            // Assert
            Truth.assertThat(actual.activeMarkets.single().outcomes.single().title)
                .isEqualTo(stringReference(model.expected))
        }

        private fun provideTestModels(): List<OutcomeLabelModel> = listOf(
            // An implied probability doubles as the outcome share price in cents.
            OutcomeLabelModel(title = "Yes", probability = BigDecimal("0.25"), expected = "Yes • 25¢"),
            // Half-up rounding to a whole cent.
            OutcomeLabelModel(title = "No", probability = BigDecimal("0.746"), expected = "No • 75¢"),
            // Without a probability the label is the bare outcome title.
            OutcomeLabelModel(title = "Yes", probability = null, expected = "Yes"),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class EventDates {

        @ParameterizedTest
        @ProvideTestModels
        fun transformResolutionDate(model: ResolutionDateModel) {
            // Act
            val actual = transform(createEvent(endDate = model.endDate))

            // Assert
            Truth.assertThat(actual.resolutionDate).isEqualTo(model.expected?.let(::stringReference))
        }

        @Test
        fun `GIVEN start date WHEN transform THEN market opened date rendered in eastern time`() {
            // Act
            val actual = transform(createEvent(startDate = "2026-07-11T22:00:00Z"))

            // Assert
            Truth.assertThat(actual.marketOpenedDate).isEqualTo(stringReference("Jul 11, 2026, 6:00 PM ET"))
        }

        private fun provideTestModels(): List<ResolutionDateModel> = listOf(
            // A summer instant renders in EDT (UTC-4).
            ResolutionDateModel(endDate = "2026-07-11T22:00:00Z", expected = "Jul 11, 2026, 6:00 PM ET"),
            // A winter instant renders in EST (UTC-5).
            ResolutionDateModel(endDate = "2026-01-15T23:00:00Z", expected = "Jan 15, 2026, 6:00 PM ET"),
            // The upstream date format is not confirmed, so an unparseable value hides the row instead of crashing.
            ResolutionDateModel(endDate = "tomorrow", expected = null),
            ResolutionDateModel(endDate = null, expected = null),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Change24h {

        // The ratio is asserted directly: going through `transform` would also format the total
        // volume via `compact()`, which relies on android.icu and is unavailable to JVM unit tests.

        @ParameterizedTest
        @ProvideTestModels
        fun computeChange24hRatio(model: Change24hModel) {
            // Act
            val actual = PolymarketEventDetailsContentTransformer.computeChange24hRatio(
                volume = model.volume,
                volume24h = model.volume24h,
            )

            // Assert
            Truth.assertThat(actual?.stripTrailingZeros())
                .isEqualTo(model.expected?.stripTrailingZeros())
        }

        private fun provideTestModels(): List<Change24hModel> = listOf(
            // The delta is the growth of the total volume: volume24h against the day-old base.
            Change24hModel(volume = BigDecimal("102"), volume24h = BigDecimal("2"), expected = BigDecimal("0.02")),
            Change24hModel(
                volume = BigDecimal("6300000"),
                volume24h = BigDecimal("128400"),
                // 128400 / 6171600, the 2.08% of the design mock.
                expected = BigDecimal("0.02080497763951001"),
            ),
            // Unknown volumes leave nothing to derive from.
            Change24hModel(volume = null, volume24h = BigDecimal("2"), expected = null),
            Change24hModel(volume = BigDecimal("102"), volume24h = null, expected = null),
            // An event that traded its whole volume within the window has no base to grow from.
            Change24hModel(volume = BigDecimal("100"), volume24h = BigDecimal("100"), expected = null),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ContentFields {

        @Test
        fun `GIVEN blank description WHEN transform THEN description is null`() {
            // Act
            val actual = transform(createEvent(description = " "))

            // Assert
            Truth.assertThat(actual.description).isNull()
        }

        @Test
        fun `GIVEN market without icon WHEN transform THEN card falls back to the event icon`() {
            // Arrange
            val event = createEvent(
                iconUrl = "https://img/event.png",
                markets = listOf(createMarket(iconUrl = null)),
            )

            // Act
            val actual = transform(event)

            // Assert
            Truth.assertThat(actual.activeMarkets.single().iconUrl).isEqualTo("https://img/event.png")
        }

        @Test
        fun `GIVEN fold flags WHEN transform THEN they pass through to the state`() {
            // Act
            val actual = transform(
                event = createEvent(),
                isClosedMarketsExpanded = true,
                isDescriptionExpanded = true,
            )

            // Assert
            Truth.assertThat(actual.isClosedMarketsExpanded).isTrue()
            Truth.assertThat(actual.isDescriptionExpanded).isTrue()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Callbacks {

        @Test
        fun `GIVEN transformed event WHEN share clicked THEN slug is reported`() {
            // Act
            transform(createEvent(slug = "world-cup-2026")).onShareClick()

            // Assert
            Truth.assertThat(sharedSlugs).containsExactly("world-cup-2026")
        }

        @Test
        fun `GIVEN transformed event WHEN outcome clicked THEN market and asset ids are reported`() {
            // Arrange
            val event = createEvent(
                markets = listOf(createMarket(id = "market-1", outcomes = listOf(createOutcome(assetId = "asset-1")))),
            )

            // Act
            transform(event).activeMarkets.single().outcomes.single().onClick()

            // Assert
            Truth.assertThat(clickedOutcomes).containsExactly("market-1" to "asset-1")
        }
    }

    internal data class OutcomeLabelModel(
        val title: String,
        val probability: BigDecimal?,
        val expected: String,
    )

    internal data class ResolutionDateModel(
        val endDate: String?,
        val expected: String?,
    )

    internal data class Change24hModel(
        val volume: BigDecimal?,
        val volume24h: BigDecimal?,
        val expected: BigDecimal?,
    )

    @Suppress("LongParameterList")
    private fun createEvent(
        slug: String = "event-slug",
        description: String = "Event description",
        iconUrl: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        volume: BigDecimal? = null,
        volume24h: BigDecimal? = null,
        displayMode: PolymarketDisplayMode = PolymarketDisplayMode.GROUPED_OUTCOMES,
        markets: List<PolymarketMarket> = listOf(createMarket()),
    ): PolymarketEvent = PolymarketEvent(
        id = "event-1",
        slug = slug,
        title = "Event title",
        description = description,
        rulesUrl = "https://polymarket.com/rules",
        iconUrl = iconUrl,
        imageUrl = null,
        status = PolymarketStatus.ACTIVE,
        startDate = startDate,
        endDate = endDate,
        volume = volume,
        volume24h = volume24h,
        liquidity = null,
        totalMarketsCount = markets.size,
        isNegRisk = displayMode == PolymarketDisplayMode.GROUPED_OUTCOMES,
        displayMode = displayMode,
        markets = markets,
    )

    private fun createMarket(
        id: String = "market-1",
        title: String = "Market question",
        groupItemTitle: String? = null,
        iconUrl: String? = null,
        status: PolymarketStatus = PolymarketStatus.ACTIVE,
        orderIndex: Int = 0,
        outcomes: List<PolymarketOutcome> = listOf(createOutcome()),
    ): PolymarketMarket = PolymarketMarket(
        id = id,
        eventId = "event-1",
        title = title,
        slug = "market-slug",
        description = "Market description",
        groupItemTitle = groupItemTitle,
        iconUrl = iconUrl,
        imageUrl = null,
        status = status,
        isNegRisk = false,
        startDate = null,
        endDate = null,
        startDateIso = null,
        endDateIso = null,
        volume = null,
        volume24h = null,
        liquidity = null,
        orderIndex = orderIndex,
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