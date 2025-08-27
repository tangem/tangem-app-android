package com.tangem.features.swap.v2.impl.sendviaswap.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AnalyticsParam.Key.FEE_TYPE
import com.tangem.core.analytics.models.AnalyticsParam.Key.PROVIDER
import com.tangem.core.analytics.models.AnalyticsParam.Key.RECEIVE_BLOCKCHAIN
import com.tangem.core.analytics.models.AnalyticsParam.Key.RECEIVE_TOKEN
import com.tangem.core.analytics.models.AnalyticsParam.Key.SEND_BLOCKCHAIN
import com.tangem.core.analytics.models.AnalyticsParam.Key.SEND_TOKEN
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.send.v2.api.analytics.CommonSendAnalyticEvents

internal sealed class SendWithSwapAnalyticEvents(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category = CommonSendAnalyticEvents.SEND_CATEGORY, event = event, params = params) {

    data class TransactionScreenOpened(
        val providerName: String,
        val feeType: AnalyticsParam.FeeType,
        val fromToken: CryptoCurrency,
        val toToken: CryptoCurrency,
    ) : SendWithSwapAnalyticEvents(
        event = "Send With Swap In Progress Screen Opened",
        params = mapOf(
            PROVIDER to providerName,
            FEE_TYPE to if (feeType is AnalyticsParam.FeeType.Normal) "Market" else "Fast",
            SEND_TOKEN to fromToken.symbol,
            RECEIVE_TOKEN to toToken.symbol,
            SEND_BLOCKCHAIN to fromToken.network.name,
            RECEIVE_BLOCKCHAIN to toToken.network.name,
        ),
    )
}