package com.tangem.features.swap.v2.impl.amount.analytics

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.express.models.ExpressError
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.features.swap.v2.impl.sendviaswap.analytics.SendWithSwapAnalyticEvents
import com.tangem.features.swap.v2.impl.sendviaswap.analytics.SendWithSwapAnalyticsErrorMessages

internal class SwapAmountAnalyticsSender(
    private val analyticsEventHandler: AnalyticsEventHandler,
) {

    private var lastSentErrorMessage: String? = null

    fun sendErrorIfNeeded(quotes: List<SwapQuoteUM>, selectedQuote: SwapQuoteUM?) {
        val errorMessage = resolveErrorMessage(quotes, selectedQuote)
        if (errorMessage == lastSentErrorMessage) return
        lastSentErrorMessage = errorMessage
        if (errorMessage != null) {
            analyticsEventHandler.send(
                SendWithSwapAnalyticEvents.SendWithSwapError(
                    errorScreen = SendWithSwapAnalyticEvents.ErrorScreen.Amount,
                    message = errorMessage,
                ),
            )
        }
    }

    private fun resolveErrorMessage(quotes: List<SwapQuoteUM>, selectedQuote: SwapQuoteUM?): String? {
        if (quotes.isEmpty()) return SendWithSwapAnalyticsErrorMessages.EXPRESS_QUOTE_NO_PROVIDERS
        val error = (selectedQuote as? SwapQuoteUM.Error)?.expressError ?: return null
        return when (error) {
            is ExpressError.AmountError.TooSmallError -> SendWithSwapAnalyticsErrorMessages.MIN_AMOUNT
            is ExpressError.AmountError.TooBigError -> SendWithSwapAnalyticsErrorMessages.MAX_AMOUNT
            else -> "${SendWithSwapAnalyticsErrorMessages.EXPRESS_QUOTE}: code=${error.code}"
        }
    }
}