package com.tangem.features.swap.v2.impl.amount.analytics

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.features.swap.v2.impl.sendviaswap.analytics.SendWithSwapAnalyticEvents

internal class SwapAmountAnalyticsSender(
    private val analyticsEventHandler: AnalyticsEventHandler,
) {

    private var lastSentEvent: AnalyticsEvent? = null

    fun sendErrorIfNeeded(
        quotes: List<SwapQuoteUM>,
        selectedQuote: SwapQuoteUM?,
        fromToken: CryptoCurrency,
        toToken: CryptoCurrency,
        hasInsufficientBalance: Boolean,
    ) {
        val event = resolveEvent(
            quotes = quotes,
            selectedQuote = selectedQuote,
            fromToken = fromToken,
            toToken = toToken,
            hasInsufficientBalance = hasInsufficientBalance,
        )
        if (event?.event == lastSentEvent?.event && event?.params == lastSentEvent?.params) return
        lastSentEvent = event
        if (event != null) {
            analyticsEventHandler.send(event)
        }
    }

    private fun resolveEvent(
        quotes: List<SwapQuoteUM>,
        selectedQuote: SwapQuoteUM?,
        fromToken: CryptoCurrency,
        toToken: CryptoCurrency,
        hasInsufficientBalance: Boolean,
    ): AnalyticsEvent? {
        if (hasInsufficientBalance) {
            return SendWithSwapAnalyticEvents.ErrorInsufficientBalance(fromToken = fromToken)
        }
        if (quotes.isEmpty()) return null
        val error = (selectedQuote as? SwapQuoteUM.Error)?.expressError ?: return null
        return when (error) {
            is ExpressError.AmountError.TooSmallError ->
                SendWithSwapAnalyticEvents.ErrorMinAmount(fromToken = fromToken)
            is ExpressError.AmountError.TooBigError ->
                SendWithSwapAnalyticEvents.ErrorMaxAmount(fromToken = fromToken)
            else -> SendWithSwapAnalyticEvents.ErrorExpressQuote(
                fromToken = fromToken,
                toToken = toToken,
                errorDescription = "code=${error.code}",
            )
        }
    }
}