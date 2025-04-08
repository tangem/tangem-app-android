package com.tangem.features.send.v2.subcomponents.fee.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam

internal sealed class SendFeeAnalyticEvents(
    category: String,
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category = category, event = event, params = params) {

    abstract val categoryName: String

    /** Selected fee (send after next screen opened) */
    data class SelectedFee(
        override val categoryName: String,
        val feeType: AnalyticsParam.FeeType,
    ) : SendFeeAnalyticEvents(
        category = categoryName,
        event = "Fee Selected",
        params = mapOf("Fee Type" to feeType.value),
    )

    /** Custom fee selected */
    data class CustomFeeButtonClicked(
        override val categoryName: String,
    ) : SendFeeAnalyticEvents(
        category = categoryName,
        event = "Custom Fee Clicked",
    )

    /** Custom fee edited */
    data class GasPriceInserter(
        override val categoryName: String,
    ) : SendFeeAnalyticEvents(
        category = categoryName,
        event = "Gas Price Inserted",
    )
}