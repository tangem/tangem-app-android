package com.tangem.tap.common.analytics.events

import com.tangem.core.analytics.AnalyticsEvent

/**
 * Created by Anton Zhilenkov on 28.09.2022.
 */
sealed class Basic(
    event: String,
    params: Map<String, String> = mapOf(),
    error: Throwable? = null,
) : AnalyticsEvent("Basic", event, params, error) {

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

    class ScanError(error: Throwable) : Basic(
        event = "Scan",
        error = error,
    )
}
