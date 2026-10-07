package com.tangem.feature.swap.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.feature.swap.analytics.SwapEvents
import com.tangem.feature.swap.domain.models.domain.SwapUIMode
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

internal class SwapModelSlippageAnalyticsTest : SwapModelTestBase() {

    private val sentEvents = mutableListOf<AnalyticsEvent>()

    private val fromCurrency = currency(symbol = "ETH", networkName = "Ethereum")
    private val toCurrency = currency(symbol = "BTC", networkName = "Bitcoin")
    private val provider = swapProvider(slippage = BigDecimal("1"), isCustomSlippageSupported = true)

    private val pairParams = mapOf(
        "Send Token" to "ETH",
        "Send Blockchain" to "Ethereum",
        "Receive Token" to "BTC",
        "Receive Blockchain" to "Bitcoin",
    )

    @BeforeEach
    fun setUp() {
        setUpBase()
        sentEvents.clear()
        every { analyticsEventHandler.send(capture(sentEvents)) } just Runs
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SlippageSheet {

        @Test
        fun `GIVEN pair selected WHEN slippage clicked THEN slippage button event sent with pair params`() {
            // Arrange
            val model = modelWithPair()
            SlotTracker(model.glassSheetSlotNavigation)

            // Act
            model.onSlippageClick()

            // Assert
            val event = sentEvents.filterIsInstance<SwapEvents.ButtonSlippage>().single()
            assertThat(event.id).isEqualTo("[Swap] Button - Slippage")
            assertThat(event.params).containsExactlyEntriesIn(pairParams)
        }

        @Test
        fun `GIVEN default slippage WHEN default confirmed THEN confirm event carries provider default`() {
            // Arrange
            val model = modelWithPair()

            // Act
            model.onSlippageConfirm(customSlippage = null)

            // Assert
            val event = sentEvents.filterIsInstance<SwapEvents.ButtonSlippageConfirm>().single()
            assertThat(event.id).isEqualTo("[Swap] Button - Confirm")
            assertThat(event.params).containsExactlyEntriesIn(pairParams + ("Slippage" to "1"))
        }

        @Test
        fun `GIVEN custom slippage WHEN confirmed THEN confirm event carries custom value`() {
            // Arrange
            val model = modelWithPair()

            // Act
            model.onSlippageConfirm(customSlippage = BigDecimal("0.50"))

            // Assert
            val event = sentEvents.filterIsInstance<SwapEvents.ButtonSlippageConfirm>().single()
            assertThat(event.params["Slippage"]).isEqualTo("0.5")
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ButtonSwap {

        @Test
        fun `GIVEN slippage WHEN swap event built THEN pair params and slippage included`() {
            // Act
            val event = SwapEvents.ButtonSwapClicked(
                fromCurrency = fromCurrency,
                toCurrency = toCurrency,
                swapUIMode = SwapUIMode.entries.first(),
                slippage = BigDecimal("0.50"),
            )

            // Assert
            assertThat(event.params).containsExactlyEntriesIn(
                pairParams + mapOf("Swap type" to SwapUIMode.entries.first().key, "Slippage" to "0.5"),
            )
        }

        @Test
        fun `GIVEN no slippage WHEN swap event built THEN slippage omitted`() {
            // Act
            val event = SwapEvents.ButtonSwapClicked(
                fromCurrency = fromCurrency,
                toCurrency = toCurrency,
                swapUIMode = SwapUIMode.entries.first(),
                slippage = null,
            )

            // Assert
            assertThat(event.params).doesNotContainKey("Slippage")
        }
    }

    private fun modelWithPair(): SwapModel {
        val model = createModel()
        model.dataState = model.dataState.copy(
            fromSwapCurrencyStatus = swapCurrencyStatus(fromCurrency),
            toSwapCurrencyStatus = swapCurrencyStatus(toCurrency),
            selectedProvider = provider,
        )
        return model
    }

    private fun swapCurrencyStatus(currency: CryptoCurrency): SwapCurrencyStatus = mockk(relaxed = true) {
        every { this@mockk.currency } returns currency
    }

    private fun currency(symbol: String, networkName: String): CryptoCurrency = mockk(relaxed = true) {
        every { this@mockk.symbol } returns symbol
        every { network.name } returns networkName
    }
}