package com.tangem.features.swap.v2.impl.amount.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.features.swap.v2.impl.sendviaswap.analytics.SendWithSwapAnalyticEvents
import com.tangem.features.swap.v2.impl.sendviaswap.analytics.SendWithSwapAnalyticsErrorMessages
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Test
import java.math.BigDecimal

class SwapAmountAnalyticsSenderTest {

    private val analyticsEventHandler = mockk<AnalyticsEventHandler>(relaxed = true)
    private val sender = SwapAmountAnalyticsSender(analyticsEventHandler)

    private val testProvider = ExpressProvider(
        providerId = "test",
        name = "Test Provider",
        type = ExpressProviderType.CEX,
        imageLarge = "",
        termsOfUse = null,
        privacyPolicy = null,
        slippage = null,
    )

    @Test
    fun `GIVEN empty quotes WHEN sendErrorIfNeeded THEN send no providers error`() {
        val eventSlot = slot<AnalyticsEvent>()
        every { analyticsEventHandler.send(capture(eventSlot)) } returns Unit

        sender.sendErrorIfNeeded(quotes = emptyList(), selectedQuote = null)

        verify(exactly = 1) { analyticsEventHandler.send(any()) }
        val event = eventSlot.captured as SendWithSwapAnalyticEvents.SendWithSwapError
        assertThat(event.errorScreen).isEqualTo(SendWithSwapAnalyticEvents.ErrorScreen.Amount)
        assertThat(event.message).isEqualTo(SendWithSwapAnalyticsErrorMessages.EXPRESS_QUOTE_NO_PROVIDERS)
    }

    @Test
    fun `GIVEN too small error quote WHEN sendErrorIfNeeded THEN send min amount error`() {
        val errorQuote = SwapQuoteUM.Error(
            provider = testProvider,
            expressError = ExpressError.AmountError.TooSmallError(code = 1001, amount = BigDecimal("0.01")),
        )
        val eventSlot = slot<AnalyticsEvent>()
        every { analyticsEventHandler.send(capture(eventSlot)) } returns Unit

        sender.sendErrorIfNeeded(quotes = listOf(errorQuote), selectedQuote = errorQuote)

        verify(exactly = 1) { analyticsEventHandler.send(any()) }
        val event = eventSlot.captured as SendWithSwapAnalyticEvents.SendWithSwapError
        assertThat(event.message).isEqualTo(SendWithSwapAnalyticsErrorMessages.MIN_AMOUNT)
    }

    @Test
    fun `GIVEN too big error quote WHEN sendErrorIfNeeded THEN send max amount error`() {
        val errorQuote = SwapQuoteUM.Error(
            provider = testProvider,
            expressError = ExpressError.AmountError.TooBigError(code = 1002, amount = BigDecimal("1000")),
        )
        val eventSlot = slot<AnalyticsEvent>()
        every { analyticsEventHandler.send(capture(eventSlot)) } returns Unit

        sender.sendErrorIfNeeded(quotes = listOf(errorQuote), selectedQuote = errorQuote)

        verify(exactly = 1) { analyticsEventHandler.send(any()) }
        val event = eventSlot.captured as SendWithSwapAnalyticEvents.SendWithSwapError
        assertThat(event.message).isEqualTo(SendWithSwapAnalyticsErrorMessages.MAX_AMOUNT)
    }

    @Test
    fun `GIVEN unknown express error WHEN sendErrorIfNeeded THEN send express quote error with code`() {
        val errorQuote = SwapQuoteUM.Error(
            provider = testProvider,
            expressError = ExpressError.InternalError(code = 500),
        )
        val eventSlot = slot<AnalyticsEvent>()
        every { analyticsEventHandler.send(capture(eventSlot)) } returns Unit

        sender.sendErrorIfNeeded(quotes = listOf(errorQuote), selectedQuote = errorQuote)

        verify(exactly = 1) { analyticsEventHandler.send(any()) }
        val event = eventSlot.captured as SendWithSwapAnalyticEvents.SendWithSwapError
        assertThat(event.message).isEqualTo("${SendWithSwapAnalyticsErrorMessages.EXPRESS_QUOTE}: code=500")
    }

    @Test
    fun `GIVEN content quote WHEN sendErrorIfNeeded THEN do not send analytics`() {
        val contentQuote = mockk<SwapQuoteUM.Content>()

        sender.sendErrorIfNeeded(quotes = listOf(contentQuote), selectedQuote = contentQuote)

        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN same error twice WHEN sendErrorIfNeeded THEN send analytics only once`() {
        val errorQuote = SwapQuoteUM.Error(
            provider = testProvider,
            expressError = ExpressError.AmountError.TooSmallError(code = 1001, amount = BigDecimal("0.01")),
        )

        sender.sendErrorIfNeeded(quotes = listOf(errorQuote), selectedQuote = errorQuote)
        sender.sendErrorIfNeeded(quotes = listOf(errorQuote), selectedQuote = errorQuote)

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

        sender.sendErrorIfNeeded(quotes = listOf(smallError), selectedQuote = smallError)
        sender.sendErrorIfNeeded(quotes = listOf(bigError), selectedQuote = bigError)

        verify(exactly = 2) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN error then success then same error WHEN sendErrorIfNeeded THEN send analytics twice`() {
        val errorQuote = SwapQuoteUM.Error(
            provider = testProvider,
            expressError = ExpressError.AmountError.TooSmallError(code = 1001, amount = BigDecimal("0.01")),
        )
        val contentQuote = mockk<SwapQuoteUM.Content>()

        sender.sendErrorIfNeeded(quotes = listOf(errorQuote), selectedQuote = errorQuote)
        sender.sendErrorIfNeeded(quotes = listOf(contentQuote), selectedQuote = contentQuote)
        sender.sendErrorIfNeeded(quotes = listOf(errorQuote), selectedQuote = errorQuote)

        verify(exactly = 2) { analyticsEventHandler.send(any()) }
    }
}