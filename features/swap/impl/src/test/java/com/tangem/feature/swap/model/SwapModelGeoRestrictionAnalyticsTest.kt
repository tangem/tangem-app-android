package com.tangem.feature.swap.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.feature.swap.analytics.SwapEvents
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.models.states.SwapNotificationUM
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for [SwapModel.updateGeoRestrictionNotificationState].
 *
 * Quotes are reloaded on a timer, so the same [SwapNotificationUM.Error.RegionalRestriction] is rebuilt
 * on every poll. The event must be reported once per appearance instead — the guard re-arms when the
 * notification clears or when the user switches to another pair.
 */
internal class SwapModelGeoRestrictionAnalyticsTest : SwapModelTestBase() {

    private val sentEvents = mutableListOf<AnalyticsEvent>()

    private val fromCurrency = currency(symbol = "ETH", networkName = "Ethereum", id = "ethereum")
    private val toCurrency = currency(symbol = "BTC", networkName = "Bitcoin", id = "bitcoin")

    @BeforeEach
    fun setUp() {
        setUpBase()
        sentEvents.clear()
        every { analyticsEventHandler.send(capture(sentEvents)) } just Runs
    }

    @Test
    fun `GIVEN restriction notification shown WHEN state handled THEN geo restriction event sent`() {
        // Arrange
        val model = modelShowing(SwapNotificationUM.Error.RegionalRestriction)

        // Act
        model.updateGeoRestrictionNotificationState(loadedState(fromCurrency, toCurrency))

        // Assert
        assertThat(geoRestrictionEvents()).containsExactly(expectedEvent)
    }

    @Test
    fun `GIVEN no restriction notification WHEN state handled THEN no event sent`() {
        // Arrange
        val model = modelShowing(SwapNotificationUM.Warning.HighPriceImpact)

        // Act
        model.updateGeoRestrictionNotificationState(loadedState(fromCurrency, toCurrency))

        // Assert
        assertThat(geoRestrictionEvents()).isEmpty()
    }

    @Test
    fun `GIVEN restriction reported WHEN quotes polled again THEN event not repeated`() {
        // Arrange
        val model = modelShowing(SwapNotificationUM.Error.RegionalRestriction)
        val state = loadedState(fromCurrency, toCurrency)
        model.updateGeoRestrictionNotificationState(state)

        // Act
        model.updateGeoRestrictionNotificationState(state)

        // Assert
        assertThat(geoRestrictionEvents()).containsExactly(expectedEvent)
    }

    @Test
    fun `GIVEN restriction cleared WHEN it appears again THEN event sent again`() {
        // Arrange
        val model = modelShowing(SwapNotificationUM.Error.RegionalRestriction)
        val state = loadedState(fromCurrency, toCurrency)
        model.updateGeoRestrictionNotificationState(state)

        // Act
        model.uiState = model.uiState.copy(notifications = persistentListOf())
        model.updateGeoRestrictionNotificationState(state)
        model.uiState = model.uiState.copy(
            notifications = persistentListOf(SwapNotificationUM.Error.RegionalRestriction),
        )
        model.updateGeoRestrictionNotificationState(state)

        // Assert
        assertThat(geoRestrictionEvents()).containsExactly(expectedEvent, expectedEvent)
    }

    @Test
    fun `GIVEN restriction reported WHEN quotes fail and it appears again THEN event sent again`() {
        // Arrange
        val model = modelShowing(SwapNotificationUM.Error.RegionalRestriction)
        val state = loadedState(fromCurrency, toCurrency)
        model.updateGeoRestrictionNotificationState(state)

        // Act
        model.handleQuotesLoadingError(IllegalStateException("quotes are unavailable"))
        model.uiState = model.uiState.copy(
            notifications = persistentListOf(SwapNotificationUM.Error.RegionalRestriction),
        )
        model.updateGeoRestrictionNotificationState(state)

        // Assert
        assertThat(geoRestrictionEvents()).containsExactly(expectedEvent, expectedEvent)
    }

    @Test
    fun `GIVEN restriction reported WHEN pair changes THEN event sent for the new pair`() {
        // Arrange
        val model = modelShowing(SwapNotificationUM.Error.RegionalRestriction)
        model.updateGeoRestrictionNotificationState(loadedState(fromCurrency, toCurrency))
        val otherToCurrency = currency(symbol = "SOL", networkName = "Solana", id = "solana")

        // Act
        model.updateGeoRestrictionNotificationState(loadedState(fromCurrency, otherToCurrency))

        // Assert
        assertThat(geoRestrictionEvents()).containsExactly(
            expectedEvent,
            SwapEvents.NoticeGeoRestriction(
                sendToken = "ETH",
                receiveToken = "SOL",
                sendBlockchain = "Ethereum",
                receiveBlockchain = "Solana",
            ),
        )
    }

    @Test
    fun `GIVEN restriction event sent WHEN inspected THEN it carries the agreed params`() {
        // Arrange
        val model = modelShowing(SwapNotificationUM.Error.RegionalRestriction)

        // Act
        model.updateGeoRestrictionNotificationState(loadedState(fromCurrency, toCurrency))

        // Assert
        val event = geoRestrictionEvents().single()
        assertThat(event.id).isEqualTo("[Swap] Notice - Geo Restriction")
        assertThat(event.params).containsExactlyEntriesIn(
            mapOf(
                "Send Token" to "ETH",
                "Receive Token" to "BTC",
                "Send Blockchain" to "Ethereum",
                "Receive Blockchain" to "Bitcoin",
            ),
        )
    }

    private fun geoRestrictionEvents() = sentEvents.filterIsInstance<SwapEvents.NoticeGeoRestriction>()

    private val expectedEvent
        get() = SwapEvents.NoticeGeoRestriction(
            sendToken = "ETH",
            receiveToken = "BTC",
            sendBlockchain = "Ethereum",
            receiveBlockchain = "Bitcoin",
        )

    private fun modelShowing(notification: NotificationUM): SwapModel {
        val model = createModel()
        model.uiState = model.uiState.copy(notifications = persistentListOf(notification))
        return model
    }

    private fun loadedState(
        fromCurrency: CryptoCurrency,
        toCurrency: CryptoCurrency,
    ): SwapState.QuotesLoadedState = mockk(relaxed = true) {
        every { fromTokenInfo.swapCurrencyStatus.currency } returns fromCurrency
        every { toTokenInfo.swapCurrencyStatus.currency } returns toCurrency
    }

    private fun currency(symbol: String, networkName: String, id: String): CryptoCurrency = mockk(relaxed = true) {
        every { this@mockk.symbol } returns symbol
        every { network.name } returns networkName
        every { this@mockk.id.value } returns id
    }
}