package com.tangem.features.swap.v2.impl.sendviaswap.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AnalyticsParam.Key.FEE_TYPE
import com.tangem.core.analytics.models.AnalyticsParam.Key.PROVIDER
import com.tangem.core.analytics.models.AnalyticsParam.Key.RECEIVE_BLOCKCHAIN
import com.tangem.core.analytics.models.AnalyticsParam.Key.RECEIVE_TOKEN
import com.tangem.core.analytics.models.AnalyticsParam.Key.SEND_BLOCKCHAIN
import com.tangem.core.analytics.models.AnalyticsParam.Key.SEND_TOKEN
import com.tangem.core.analytics.models.AppsFlyerIncludedEvent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.send.v2.api.analytics.CommonSendAnalyticEvents

internal sealed class SendWithSwapAnalyticEvents(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = CommonSendAnalyticEvents.SEND_CATEGORY, event = event, params = params) {

    data class TransactionScreenOpened(
        val providerName: String,
        val feeType: AnalyticsParam.FeeType,
        val fromToken: CryptoCurrency,
        val toToken: CryptoCurrency,
        val fromDerivationIndex: Int?,
    ) : SendWithSwapAnalyticEvents(
        event = "Send With Swap In Progress Screen Opened",
        params = buildMap {
            put(PROVIDER, providerName)
            put(FEE_TYPE, if (feeType is AnalyticsParam.FeeType.Normal) "Market" else "Fast")
            put(SEND_TOKEN, fromToken.symbol)
            put(RECEIVE_TOKEN, toToken.symbol)
            put(SEND_BLOCKCHAIN, fromToken.network.name)
            put(RECEIVE_BLOCKCHAIN, toToken.network.name)
            if (fromDerivationIndex != null) put("Account Derivation (from)", fromDerivationIndex.toString())
        },
    ), AppsFlyerIncludedEvent

    data class NoticeCanNotSwapToken(
        val fromToken: CryptoCurrency,
        val toTokenSymbol: String,
    ) : SendWithSwapAnalyticEvents(
        event = "Notice - Can`t Swap This Token",
        params = mapOf(
            SEND_TOKEN to fromToken.symbol,
            RECEIVE_TOKEN to toTokenSymbol,
            SEND_BLOCKCHAIN to fromToken.network.name,
        ),
    )
}