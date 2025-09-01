package com.tangem.features.send.v2.api.subcomponents.amount.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam.Key.BLOCKCHAIN
import com.tangem.core.analytics.models.AnalyticsParam.Key.TOKEN_PARAM
import com.tangem.core.analytics.models.AnalyticsParam.Key.TYPE

sealed class CommonSendAmountAnalyticEvents(
    category: String,
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category = category, event = event, params = params) {

    /** Selected currency */
    data class SelectedCurrency(
        val categoryName: String,
        val type: SelectedCurrencyType,
    ) : CommonSendAmountAnalyticEvents(
        category = categoryName,
        event = "Selected Currency",
        params = mapOf(TYPE to type.value),
    )

    /** Max amount button clicked */
    data class MaxAmountButtonClicked(
        val categoryName: String,
        val token: String,
        val blockchain: String,
    ) : CommonSendAmountAnalyticEvents(
        category = categoryName,
        event = "Max Amount Taped",
        params = mapOf(
            TOKEN_PARAM to token,
            BLOCKCHAIN to blockchain,
        ),
    )

    enum class SelectedCurrencyType(val value: String) {
        Token("Token"),
        AppCurrency("App Currency"),
    }
}