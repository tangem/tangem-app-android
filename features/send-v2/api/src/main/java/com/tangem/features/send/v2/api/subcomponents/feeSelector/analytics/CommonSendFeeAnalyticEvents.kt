package com.tangem.features.send.v2.api.subcomponents.feeSelector.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AnalyticsParam.Key.FEE_TYPE
import com.tangem.core.analytics.models.AnalyticsParam.Key.SOURCE
import com.tangem.features.send.v2.api.analytics.CommonSendAnalyticEvents.CommonSendSource

sealed class CommonSendFeeAnalyticEvents(
    category: String,
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category = category, event = event, params = params) {

    abstract val categoryName: String

    /** Selected fee (send after next screen opened) */
    data class SelectedFee(
        override val categoryName: String,
        val feeType: AnalyticsParam.FeeType,
        val source: CommonSendSource,
    ) : CommonSendFeeAnalyticEvents(
        category = categoryName,
        event = "Fee Selected",
        params = mapOf(
            FEE_TYPE to feeType.value,
            SOURCE to source.analyticsName,
        ),
    )

    /** Custom fee selected */
    data class CustomFeeButtonClicked(
        override val categoryName: String,
    ) : CommonSendFeeAnalyticEvents(
        category = categoryName,
        event = "Custom Fee Clicked",
    )

    /** Custom fee edited */
    data class GasPriceInserter(
        override val categoryName: String,
    ) : CommonSendFeeAnalyticEvents(
        category = categoryName,
        event = "Gas Price Inserted",
    )
}