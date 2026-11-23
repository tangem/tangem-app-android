package com.tangem.features.earn.impl.analytics

import com.tangem.core.analytics.models.AnalyticsParam

internal sealed class EarnTxResultAnalyticsEvent(
    category: String,
    event: String,
    params: Map<String, String>,
) : EarnAnalyticsEvent(category = category, event = event, params = params) {

    class TransactionConfirmed(
        category: String,
        params: Map<String, String>,
    ) : EarnTxResultAnalyticsEvent(
        category = category,
        event = "Transaction Confirmed",
        params = params,
    )

    class TransactionFailed(
        category: String,
        params: Map<String, String>,
    ) : EarnTxResultAnalyticsEvent(
        category = category,
        event = "Error - Transaction Failed",
        params = params + (AnalyticsParam.ERROR_CODE to REVERTED),
    )

    private companion object {
        const val REVERTED = "Reverted"
    }
}