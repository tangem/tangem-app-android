package com.tangem.feature.tokendetails.presentation.tokendetails.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.feature.tokendetails.presentation.tokendetails.analytics.utils.toAnalyticsParams

internal open class TokenDetailsAnalyticsEvent(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category = "Token", event, params) {

    open class Notice(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : TokenDetailsAnalyticsEvent(event = "Notice - $event", params) {

        class NetworkUnreachable(currency: CryptoCurrency) : Notice(
            event = "Network Unreachable",
            params = currency.toAnalyticsParams(),
        )

        class NotEnoughFee(currency: CryptoCurrency) : Notice(
            event = "Not Enough Fee",
            params = currency.toAnalyticsParams(),
        )

        class Reveal(currency: CryptoCurrency) : Notice(
            event = "Reveal Transaction",
            params = currency.toAnalyticsParams(),
        )
    }
}