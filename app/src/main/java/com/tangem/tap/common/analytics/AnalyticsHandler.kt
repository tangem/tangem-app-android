package com.tangem.tap.common.analytics

import com.tangem.common.card.Card
import com.tangem.common.core.TangemSdkError
import com.tangem.tap.common.extensions.filterNotNull


abstract class AnalyticsHandler {
    abstract fun triggerEvent(
        event: AnalyticsEvent,
        card: Card? = null,
        blockchain: String? = null,
        params: Map<String, String> = emptyMap()
    )

    abstract fun logCardSdkError(
        error: TangemSdkError,
        actionToLog: Analytics.ActionToLog,
        parameters: Map<AnalyticsParam, String>? = null,
        card: Card? = null
    )

    abstract fun logError(
        error: Throwable,
        params: Map<String, String> = emptyMap()
    )

    protected fun prepareParams(card: Card?, blockchain: String? = null): Map<String, String> {
        return mapOf(
            AnalyticsParam.FIRMWARE.param to card?.firmwareVersion?.stringValue,
            AnalyticsParam.BATCH_ID.param to card?.batchId,
            AnalyticsParam.BLOCKCHAIN.param to blockchain,
        ).filterNotNull()
    }

    fun logWcEvent(event: Analytics.WcAnalyticsEvent) {
        when (event) {
            is Analytics.WcAnalyticsEvent.Action -> {
                triggerEvent(
                    event = AnalyticsEvent.WC_SUCCESS_RESPONSE,
                    params = mapOf(
                        AnalyticsParam.WALLET_CONNECT_ACTION.param to event.action.name
                    )
                )
            }
            is Analytics.WcAnalyticsEvent.Error -> {
                val params = mapOf(
                    AnalyticsParam.WALLET_CONNECT_ACTION.param to event.action?.name,
                    AnalyticsParam.ERROR_DESCRIPTION.param to event.error.message
                )
                    .filterNotNull()
                logError(event.error, params)
            }
            is Analytics.WcAnalyticsEvent.InvalidRequest ->
                triggerEvent(
                    event = AnalyticsEvent.WC_INVALID_REQUEST,
                    params = mapOf(
                        AnalyticsParam.WALLET_CONNECT_REQUEST.param to event.json
                    ).filterNotNull()
                )
            is Analytics.WcAnalyticsEvent.Session -> {
                val analyticsEvent = when (event.event) {
                    Analytics.WcSessionEvent.Disconnect -> AnalyticsEvent.WC_SESSION_DISCONNECTED
                    Analytics.WcSessionEvent.Connect -> AnalyticsEvent.WC_NEW_SESSION
                }
                triggerEvent(
                    event = analyticsEvent,
                    params = mapOf(
                        AnalyticsParam.WALLET_CONNECT_DAPP_URL.param to event.url
                    ).filterNotNull()
                )
            }
        }

    }
}