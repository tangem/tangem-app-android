package com.tangem.tap.common.analytics.events

/**
* [REDACTED_AUTHOR]
 */
sealed class Basic(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Basic", event, params) {

    sealed class SignedIn(
        state: AnalyticsParam.CardState,
        currency: AnalyticsParam.CardCurrency,
        batch: String,
    ) : Basic(
        event = "Signed in",
        params = mapOf(
            "State" to state.value,
            "Currency" to currency.value,
            "Batch" to batch,
        ),
    )

    class ToppedUp(currency: AnalyticsParam.CardCurrency) : Basic(
        event = "Topped up",
        params = mapOf("Currency" to currency.value),
    )
}

