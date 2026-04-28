package com.tangem.features.swap.v2.impl.amount.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.features.swap.v2.impl.sendviaswap.analytics.SendWithSwapAnalyticEvents
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Test
import java.math.BigDecimal

class SwapAmountAnalyticsSenderTest {

    private val analyticsEventHandler = mockk<AnalyticsEventHandler>(relaxed = true)
    private val sender = SwapAmountAnalyticsSender(analyticsEventHandler)

    private val fromNetwork = mockk<Network>(relaxed = true).also {
        every { it.name } returns "Ethereum"
    }
    private val toNetwork = mockk<Network>(relaxed = true).also {
        every { it.name } returns "Bitcoin"
    }
    private val fromToken = mockk<CryptoCurrency>(relaxed = true).also {
        every { it.symbol } returns "ETH"
        every { it.network } returns fromNetwork
    }
    private val toToken = mockk<CryptoCurrency>(relaxed = true).also {
        every { it.symbol } returns "BTC"
        every { it.network } returns toNetwork
    }

    private val testProvider = ExpressProvider(
        providerId = "test",
        name = "Test Provider",
        type = ExpressProviderType.CEX,
        imageLarge = "",
        termsOfUse = null,
        privacyPolicy = null,
        slippage = null,
    )

    private fun send(
        quotes: List<SwapQuoteUM> = emptyList(),
        selectedQuote: SwapQuoteUM? = null,
        hasInsufficientBalance: Boolean = false,
    ) = sender.sendErrorIfNeeded(
        quotes = quotes,
        selectedQuote = selectedQuote,
        fromToken = fromToken,
        toToken = toToken,
        hasInsufficientBalance = hasInsufficientBalance,
    )

    @Test
    fun `GIVEN empty quotes WHEN sendErrorIfNeeded THEN do not send analytics`() {
        send(quotes = emptyList(), selectedQuote = null)

        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN insufficient balance WHEN sendErrorIfNeeded THEN send ErrorInsufficientBalance with from token params`() {
        val eventSlot = slot<AnalyticsEvent>()
        every { analyticsEventHandler.send(capture(eventSlot)) } returns Unit

        send(hasInsufficientBalance = true)

        verify(exactly = 1) { analyticsEventHandler.send(any()) }
        val event = eventSlot.captured
        assertThat(event).isInstanceOf(SendWithSwapAnalyticEvents.ErrorInsufficientBalance::class.java)
        assertThat(event.event).isEqualTo("Error - Insufficient balance")
        assertThat(event.params).containsExactly("Send Token", "ETH", "Send Blockchain", "Ethereum")
    }

    @Test
    fun `GIVEN insufficient balance and express error WHEN sendErrorIfNeeded THEN insufficient balance takes priority`() {
        val errorQuote = SwapQuoteUM.Error(
            provider = testProvider,
            expressError = ExpressError.AmountError.TooSmallError(code = 1001, amount = BigDecimal("0.01")),
        )
        val eventSlot = slot<AnalyticsEvent>()
        every { analyticsEventHandler.send(capture(eventSlot)) } returns Unit

        send(quotes = listOf(errorQuote), selectedQuote = errorQuote, hasInsufficientBalance = true)

        verify(exactly = 1) { analyticsEventHandler.send(any()) }
        assertThat(eventSlot.captured).isInstanceOf(SendWithSwapAnalyticEvents.ErrorInsufficientBalance::class.java)
    }

    @Test
    fun `GIVEN too small error quote WHEN sendErrorIfNeeded THEN send ErrorMinAmount with from token params`() {
        val errorQuote = SwapQuoteUM.Error(
            provider = testProvider,
            expressError = ExpressError.AmountError.TooSmallError(code = 1001, amount = BigDecimal("0.01")),
        )
        val eventSlot = slot<AnalyticsEvent>()
        every { analyticsEventHandler.send(capture(eventSlot)) } returns Unit

        send(quotes = listOf(errorQuote), selectedQuote = errorQuote)

        verify(exactly = 1) { analyticsEventHandler.send(any()) }
        val event = eventSlot.captured
        assertThat(event).isInstanceOf(SendWithSwapAnalyticEvents.ErrorMinAmount::class.java)
        assertThat(event.event).isEqualTo("Error - Min amount")
        assertThat(event.params).containsExactly("Send Token", "ETH", "Send Blockchain", "Ethereum")
    }

    @Test
    fun `GIVEN too big error quote WHEN sendErrorIfNeeded THEN send ErrorMaxAmount with from token params`() {
        val errorQuote = SwapQuoteUM.Error(
            provider = testProvider,
            expressError = ExpressError.AmountError.TooBigError(code = 1002, amount = BigDecimal("1000")),
        )
        val eventSlot = slot<AnalyticsEvent>()
        every { analyticsEventHandler.send(capture(eventSlot)) } returns Unit

        send(quotes = listOf(errorQuote), selectedQuote = errorQuote)

        verify(exactly = 1) { analyticsEventHandler.send(any()) }
        val event = eventSlot.captured
        assertThat(event).isInstanceOf(SendWithSwapAnalyticEvents.ErrorMaxAmount::class.java)
        assertThat(event.event).isEqualTo("Error - Max amount")
        assertThat(event.params).containsExactly("Send Token", "ETH", "Send Blockchain", "Ethereum")
    }

    @Test
    fun `GIVEN unknown express error WHEN sendErrorIfNeeded THEN send ErrorExpressQuote with both tokens and code`() {
        val errorQuote = SwapQuoteUM.Error(
            provider = testProvider,
            expressError = ExpressError.InternalError(code = 500),
        )
        val eventSlot = slot<AnalyticsEvent>()
        every { analyticsEventHandler.send(capture(eventSlot)) } returns Unit

        send(quotes = listOf(errorQuote), selectedQuote = errorQuote)

        verify(exactly = 1) { analyticsEventHandler.send(any()) }
        val event = eventSlot.captured
        assertThat(event).isInstanceOf(SendWithSwapAnalyticEvents.ErrorExpressQuote::class.java)
        assertThat(event.event).isEqualTo("Error - Express quote")
        assertThat(event.params).containsExactly(
            "Send Token", "ETH",
            "Send Blockchain", "Ethereum",
            "Receive Token", "BTC",
            "Receive Blockchain", "Bitcoin",
            "Error Description", "code=500",
        )
    }

    @Test
    fun `GIVEN content quote WHEN sendErrorIfNeeded THEN do not send analytics`() {
        val contentQuote = mockk<SwapQuoteUM.Content>()

        send(quotes = listOf(contentQuote), selectedQuote = contentQuote)

        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN same error twice WHEN sendErrorIfNeeded THEN send analytics only once`() {
        val errorQuote = SwapQuoteUM.Error(
            provider = testProvider,
            expressError = ExpressError.AmountError.TooSmallError(code = 1001, amount = BigDecimal("0.01")),
        )

        send(quotes = listOf(errorQuote), selectedQuote = errorQuote)
        send(quotes = listOf(errorQuote), selectedQuote = errorQuote)

        verify(exactly = 1) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN insufficient balance twice WHEN sendErrorIfNeeded THEN send analytics only once`() {
        send(hasInsufficientBalance = true)
        send(hasInsufficientBalance = true)

        verify(exactly = 1) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN error then different error WHEN sendErrorIfNeeded THEN send analytics twice`() {
        val smallError = SwapQuoteUM.Error(
            provider = testProvider,
            expressError = ExpressError.AmountError.TooSmallError(code = 1001, amount = BigDecimal("0.01")),
        )
        val bigError = SwapQuoteUM.Error(
            provider = testProvider,
            expressError = ExpressError.AmountError.TooBigError(code = 1002, amount = BigDecimal("1000")),
        )

        send(quotes = listOf(smallError), selectedQuote = smallError)
        send(quotes = listOf(bigError), selectedQuote = bigError)

        verify(exactly = 2) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN express error then insufficient balance WHEN sendErrorIfNeeded THEN send analytics twice`() {
        val smallError = SwapQuoteUM.Error(
            provider = testProvider,
            expressError = ExpressError.AmountError.TooSmallError(code = 1001, amount = BigDecimal("0.01")),
        )

        send(quotes = listOf(smallError), selectedQuote = smallError)
        send(quotes = listOf(smallError), selectedQuote = smallError, hasInsufficientBalance = true)

        verify(exactly = 2) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN error then success then same error WHEN sendErrorIfNeeded THEN send analytics twice`() {
        val errorQuote = SwapQuoteUM.Error(
            provider = testProvider,
            expressError = ExpressError.AmountError.TooSmallError(code = 1001, amount = BigDecimal("0.01")),
        )
        val contentQuote = mockk<SwapQuoteUM.Content>()

        send(quotes = listOf(errorQuote), selectedQuote = errorQuote)
        send(quotes = listOf(contentQuote), selectedQuote = contentQuote)
        send(quotes = listOf(errorQuote), selectedQuote = errorQuote)

        verify(exactly = 2) { analyticsEventHandler.send(any()) }
    }
}