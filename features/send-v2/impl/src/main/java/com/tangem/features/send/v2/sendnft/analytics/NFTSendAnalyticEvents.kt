package com.tangem.features.send.v2.sendnft.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AnalyticsParam.Key.BLOCKCHAIN
import com.tangem.core.analytics.models.AnalyticsParam.Key.FEE_TYPE
import com.tangem.core.analytics.models.AnalyticsParam.Key.NONCE
import com.tangem.core.analytics.models.AnalyticsParam.Key.TOKEN_PARAM
import com.tangem.core.ui.extensions.capitalize
import com.tangem.features.send.v2.api.analytics.CommonSendAnalyticEvents

/**
 * Send screen analytics
 */
internal sealed class NFTSendAnalyticEvents(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category = CommonSendAnalyticEvents.NFT_SEND_CATEGORY, event = event, params = params) {

    /** Transaction send screen opened */
    data class TransactionScreenOpened(
        val token: String,
        val feeType: AnalyticsParam.FeeType,
        val blockchain: String,
        val nonceNotEmpty: Boolean,
    ) : NFTSendAnalyticEvents(
        event = "NFT Sent Screen Opened",
        params = mapOf(
            TOKEN_PARAM to token,
            FEE_TYPE to feeType.value,
            BLOCKCHAIN to blockchain,
            NONCE to nonceNotEmpty.toString().capitalize(),
        ),
    )
}