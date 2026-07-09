package com.tangem.features.onramp.utils

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.onramp.analytics.OnrampAnalyticsEvent
import com.tangem.domain.onramp.model.OnrampQuote

internal fun AnalyticsEventHandler.sendProviderCalculatedEvent(quotes: List<OnrampQuote>, tokenSymbol: String) {
    val quote = quotes.findBestRateQuote() ?: return

    send(
        OnrampAnalyticsEvent.ProviderCalculated(
            providerName = quote.provider.info.name,
            tokenSymbol = tokenSymbol,
            paymentMethod = quote.paymentMethod.name,
        ),
    )
}

private fun List<OnrampQuote>.findBestRateQuote(): OnrampQuote.Data? {
    return filterIsInstance<OnrampQuote.Data>().maxByOrNull { it.toAmount.value }
}