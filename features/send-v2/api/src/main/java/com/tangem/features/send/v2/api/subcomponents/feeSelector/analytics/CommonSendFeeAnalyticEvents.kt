package com.tangem.features.send.v2.api.subcomponents.feeSelector.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AnalyticsParam.Key.BLOCKCHAIN
import com.tangem.core.analytics.models.AnalyticsParam.Key.FEE_TOKEN
import com.tangem.core.analytics.models.AnalyticsParam.Key.FEE_TYPE
import com.tangem.core.analytics.models.AnalyticsParam.Key.SOURCE
import com.tangem.features.send.v2.api.analytics.CommonSendAnalyticEvents.CommonSendSource

sealed class CommonSendFeeAnalyticEvents(
    category: String,
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = category, event = event, params = params) {

    abstract val categoryName: String

    /** Selected fee (send after next screen opened) */
    data class SelectedFee(
        override val categoryName: String,
        val feeType: AnalyticsParam.FeeType,
        val source: CommonSendSource,
        val feeToken: String,
        val blockchain: String,
    ) : CommonSendFeeAnalyticEvents(
        category = categoryName,
        event = "Fee Selected",
        params = mapOf(
            FEE_TYPE to feeType.value,
            SOURCE to source.analyticsName,
            FEE_TOKEN to feeToken,
            BLOCKCHAIN to blockchain,
        ),
    )

    /** Custom fee selected */
    data class CustomFeeButtonClicked(
        override val categoryName: String,
        val blockchain: String,
    ) : CommonSendFeeAnalyticEvents(
        category = categoryName,
        event = "Custom Fee Clicked",
        params = mapOf(
            BLOCKCHAIN to blockchain,
        ),
    )

    /** Custom fee edited */
    data class GasPriceInserter(
        override val categoryName: String,
    ) : CommonSendFeeAnalyticEvents(
        category = categoryName,
        event = "Gas Price Inserted",
    )
}