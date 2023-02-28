package com.tangem.tap.common.analytics.events

import com.tangem.core.analytics.AnalyticsEvent

/**
* [REDACTED_AUTHOR]
 */
sealed class Basic(
    event: String,
    params: Map<String, String> = mapOf(),
    error: Throwable? = null,
) : AnalyticsEvent("Basic", event, params, error) {

    class CardWasScanned(
        source: AnalyticsParam.ScannedFrom,
    ) : Basic(
        event = "Card Was Scanned",
        params = mapOf(
            "Source" to source.value,
        ),
    )

    class SignedIn(
        state: AnalyticsParam.CardBalanceState,
        currency: AnalyticsParam.CardCurrency,
        batch: String,
    ) : Basic(
        event = "Signed in",
        params = mapOf(
            "State" to state.value,
            AnalyticsParam.Currency to currency.value,
            AnalyticsParam.Batch to batch,
        ),
    )

    class ToppedUp(currency: AnalyticsParam.CardCurrency) : Basic(
        event = "Topped up",
        params = mapOf(AnalyticsParam.Currency to currency.value),
    )

    class ScanError(error: Throwable) : Basic(
        event = "Scan",
        error = error,
    )
}
