package com.tangem.feature.swap.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.models.states.SwapNotificationUM
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class LowSlippageNoticeAnalyticsTest {

    private val analyticsEventHandler: AnalyticsEventHandler = mockk()
    private val sentEvents = mutableListOf<AnalyticsEvent>()

    private val lowSlippage = BigDecimal("0.1")
    private val notices = listOf(SwapNotificationUM.Warning.LowSlippage(stringReference("0.1%")))
    private val state: SwapState.QuotesLoadedState = mockk(relaxed = true) {
        every { fromTokenInfo.swapCurrencyStatus.currency } returns currency(symbol = "ETH", networkName = "Ethereum")
        every { toTokenInfo.swapCurrencyStatus.currency } returns currency(symbol = "BTC", networkName = "Bitcoin")
    }

    private val analytics = LowSlippageNoticeAnalytics(analyticsEventHandler)

    @BeforeEach
    fun setUp() {
        clearMocks(analyticsEventHandler)
        sentEvents.clear()
        analytics.reset()
        every { analyticsEventHandler.send(capture(sentEvents)) } just Runs
    }

    @Test
    fun `GIVEN low slippage notice shown WHEN quotes loaded THEN warning sent with pair and slippage`() {
        // Act
        analytics.onQuotesLoaded(state = state, notifications = notices, customSlippage = lowSlippage)

        // Assert
        val event = sentEvents.single()
        assertThat(event.id).isEqualTo("[Swap] Warning - Custom slippage threshold")
        assertThat(event.params).containsExactlyEntriesIn(
            mapOf(
                "Send Token" to "ETH",
                "Send Blockchain" to "Ethereum",
                "Receive Token" to "BTC",
                "Receive Blockchain" to "Bitcoin",
                "Slippage" to "0.1",
            ),
        )
    }

    @Test
    fun `GIVEN warning reported WHEN quotes polled again THEN warning not repeated`() {
        // Arrange
        analytics.onQuotesLoaded(state = state, notifications = notices, customSlippage = lowSlippage)

        // Act
        analytics.onQuotesLoaded(state = state, notifications = notices, customSlippage = lowSlippage)

        // Assert
        assertThat(sentEvents).hasSize(1)
    }

    @Test
    fun `GIVEN warning reported WHEN notice clears and appears again THEN warning sent again`() {
        // Arrange
        analytics.onQuotesLoaded(state = state, notifications = notices, customSlippage = lowSlippage)

        // Act
        analytics.onQuotesLoaded(state = state, notifications = emptyList(), customSlippage = lowSlippage)
        analytics.onQuotesLoaded(state = state, notifications = notices, customSlippage = lowSlippage)

        // Assert
        assertThat(sentEvents).hasSize(2)
    }

    @Test
    fun `GIVEN warning reported WHEN reset and notice shown again THEN warning sent again`() {
        // Arrange
        analytics.onQuotesLoaded(state = state, notifications = notices, customSlippage = lowSlippage)

        // Act
        analytics.reset()
        analytics.onQuotesLoaded(state = state, notifications = notices, customSlippage = lowSlippage)

        // Assert
        assertThat(sentEvents).hasSize(2)
    }

    @Test
    fun `GIVEN warning reported WHEN another low slippage set THEN warning sent for the new value`() {
        // Arrange
        analytics.onQuotesLoaded(state = state, notifications = notices, customSlippage = lowSlippage)

        // Act
        analytics.onQuotesLoaded(state = state, notifications = notices, customSlippage = BigDecimal("0.2"))

        // Assert
        assertThat(sentEvents.map { it.params["Slippage"] }).containsExactly("0.1", "0.2").inOrder()
    }

    @Test
    fun `GIVEN no low slippage notice WHEN quotes loaded THEN no warning sent`() {
        // Act
        analytics.onQuotesLoaded(state = state, notifications = emptyList(), customSlippage = lowSlippage)

        // Assert
        assertThat(sentEvents).isEmpty()
    }

    private fun currency(symbol: String, networkName: String): CryptoCurrency = mockk(relaxed = true) {
        every { this@mockk.symbol } returns symbol
        every { network.name } returns networkName
    }
}