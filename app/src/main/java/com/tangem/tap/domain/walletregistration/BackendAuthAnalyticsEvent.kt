package com.tangem.tap.domain.walletregistration

import com.tangem.core.analytics.models.AnalyticsEvent

/**
 * Backend-authentication analytics events (category "Backend Auth").
 */
internal sealed class BackendAuthAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = "Backend Auth", event = event, params = params) {

    class UnexpectedWalletCurve(cardId: String) : BackendAuthAnalyticsEvent(
        event = "Unexpected Wallet Curve",
        params = mapOf("Card Id" to cardId),
    )
}