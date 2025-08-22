package com.tangem.features.send.v2.send.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AnalyticsParam.Key.BLOCKCHAIN
import com.tangem.core.analytics.models.AnalyticsParam.Key.ENS_ADDRESS
import com.tangem.core.analytics.models.AnalyticsParam.Key.FEE_TYPE
import com.tangem.core.analytics.models.AnalyticsParam.Key.NONCE
import com.tangem.core.analytics.models.AnalyticsParam.Key.TOKEN_PARAM
import com.tangem.core.ui.extensions.capitalize
import com.tangem.features.send.v2.api.analytics.CommonSendAnalyticEvents

/**
 * Send screen analytics
 */
internal sealed class SendAnalyticEvents(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category = CommonSendAnalyticEvents.SEND_CATEGORY, event = event, params = params) {

    /** Transaction send screen opened */
    data class TransactionScreenOpened(
        val token: String,
        val feeType: AnalyticsParam.FeeType,
        val blockchain: String,
        val nonceNotEmpty: Boolean,
        private val ensStatus: AnalyticsParam.EnsStatus,
    ) : SendAnalyticEvents(
        event = "Transaction Sent Screen Opened",
        params = mapOf(
            TOKEN_PARAM to token,
            FEE_TYPE to feeType.value,
            BLOCKCHAIN to blockchain,
            NONCE to nonceNotEmpty.toString().capitalize(),
            ENS_ADDRESS to when (ensStatus) {
                AnalyticsParam.EnsStatus.EMPTY -> false.toString()
                AnalyticsParam.EnsStatus.FULL -> true.toString()
            },
        ),
    )

    data class ConvertTokenButtonClicked(
        val token: String,
        val blockchain: String,
    ) : SendAnalyticEvents(
        event = "Button - Convert Token",
        params = mapOf(
            TOKEN_PARAM to token,
            BLOCKCHAIN to blockchain,
        ),
    )
}