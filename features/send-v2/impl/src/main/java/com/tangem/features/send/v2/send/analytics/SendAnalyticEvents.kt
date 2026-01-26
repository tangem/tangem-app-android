package com.tangem.features.send.v2.send.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AnalyticsParam.Key.ACCOUNT_DERIVATION_FROM
import com.tangem.core.analytics.models.AnalyticsParam.Key.BLOCKCHAIN
import com.tangem.core.analytics.models.AnalyticsParam.Key.ENS_ADDRESS
import com.tangem.core.analytics.models.AnalyticsParam.Key.FEE_TYPE
import com.tangem.core.analytics.models.AnalyticsParam.Key.NONCE
import com.tangem.core.analytics.models.AnalyticsParam.Key.TOKEN_PARAM
import com.tangem.core.analytics.models.AppsFlyerIncludedEvent
import com.tangem.core.ui.extensions.capitalize
import com.tangem.features.send.v2.api.analytics.CommonSendAnalyticEvents

/**
 * Send screen analytics
 */
internal sealed class SendAnalyticEvents(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = CommonSendAnalyticEvents.SEND_CATEGORY, event = event, params = params) {

    /** Transaction send screen opened */
    data class TransactionScreenOpened(
        val token: String,
        val feeType: AnalyticsParam.FeeType,
        val blockchain: String,
        val isNonceNotEmpty: Boolean,
        private val ensStatus: AnalyticsParam.EmptyFull,
        private val feeToken: String,
        val derivationIndex: Int?,
    ) : SendAnalyticEvents(
        event = "Transaction Sent Screen Opened",
        params = buildMap {
            put(TOKEN_PARAM, token)
            put(FEE_TYPE, feeType.value)
            put(BLOCKCHAIN, blockchain)
            if (derivationIndex != null) put(ACCOUNT_DERIVATION_FROM, derivationIndex.toString())
            put(NONCE, isNonceNotEmpty.toString().capitalize())
            val ensAddress = when (ensStatus) {
                AnalyticsParam.EmptyFull.Empty -> false.toString().capitalize()
                AnalyticsParam.EmptyFull.Full -> true.toString().capitalize()
            }
            put(ENS_ADDRESS, ensAddress)
        },
    ), AppsFlyerIncludedEvent

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