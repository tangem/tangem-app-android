package com.tangem.tap.common.analytics.events

/**
* [REDACTED_AUTHOR]
 */
sealed class Basic(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Basic", event, params) {

    class SignedIn(
        state: AnalyticsParam.CardBalanceState,
        currency: AnalyticsParam.CardCurrency,
        batch: String,
    ) : Basic(
        event = "Signed in",
        params = mapOf(
            "State" to state.value,
            "Currency" to currency.value,
            AnalyticsParam.BatchId to batch,
        ),
    )

    class ToppedUp(currency: AnalyticsParam.CardCurrency) : Basic(
        event = "Topped up",
        params = mapOf("Currency" to currency.value),
    )
}

