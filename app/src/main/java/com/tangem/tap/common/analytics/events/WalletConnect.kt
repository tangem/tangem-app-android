package com.tangem.tap.common.analytics.events

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.tap.common.extensions.filterNotNull

/**
[REDACTED_AUTHOR]
 */
sealed class WalletConnect(
    event: String,
    params: Map<String, String> = mapOf(),
    error: Throwable? = null,
) : AnalyticsEvent("Wallet Connect", event, params, error) {

    class ScreenOpened : WalletConnect(event = "WC Screen Opened")
    class NewSessionEstablished(dAppName: String, dAppUrl: String) : WalletConnect(
        event = "New Session Established",
        params = mapOf(
            AnalyticsParam.DAPP_NAME to dAppName,
            AnalyticsParam.DAPP_URL to dAppUrl,
        ),
    )

    class SessionDisconnected(dAppName: String, dAppUrl: String) : WalletConnect(
        event = "Session Disconnected",
        params = mapOf(
            AnalyticsParam.DAPP_NAME to dAppName,
            AnalyticsParam.DAPP_URL to dAppUrl,
        ),
    )

    class RequestHandled(
        params: RequestHandledParams,
    ) : WalletConnect(
        event = "Request Handled",
        params = params.toParamsMap(),
    )

    data class RequestHandledParams(
        val dAppName: String,
        val dAppUrl: String,
        val methodName: String,
        val blockchain: String,
        val errorCode: String? = null,
    ) {
        fun toParamsMap(): Map<String, String> {
            val validation = if (errorCode == null) Validation.SUCCESS.param else Validation.FAIL.param
            return mapOf(
                AnalyticsParam.DAPP_NAME to dAppName,
                AnalyticsParam.DAPP_URL to dAppUrl,
                AnalyticsParam.METHOD_NAME to methodName,
                AnalyticsParam.BLOCKCHAIN to blockchain,
                AnalyticsParam.VALIDATION to validation,
                if (errorCode != null) AnalyticsParam.ERROR_CODE to errorCode else null to null,
            ).filterNotNull()
        }
    }

    enum class Validation(val param: String) {
        SUCCESS("Success"),
        FAIL("Fail"),
    }
}