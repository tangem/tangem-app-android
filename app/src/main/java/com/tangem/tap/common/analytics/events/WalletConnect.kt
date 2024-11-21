package com.tangem.tap.common.analytics.events

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.EventValue

/**
[REDACTED_AUTHOR]
 */
internal sealed class WalletConnect(
    event: String,
    params: Map<String, EventValue> = mapOf(),
    error: Throwable? = null,
) : AnalyticsEvent("Wallet Connect", event, params, error) {

    class ScreenOpened : WalletConnect(event = "WC Screen Opened")
    class NewSessionEstablished(dAppName: String, dAppUrl: String) : WalletConnect(
        event = "New Session Established",
        params = mapOf(
            AnalyticsParam.DAPP_NAME to dAppName.asStringValue(),
            AnalyticsParam.DAPP_URL to dAppUrl.asStringValue(),
        ),
    )

    class SessionDisconnected(dAppName: String, dAppUrl: String) : WalletConnect(
        event = "Session Disconnected",
        params = mapOf(
            AnalyticsParam.DAPP_NAME to dAppName.asStringValue(),
            AnalyticsParam.DAPP_URL to dAppUrl.asStringValue(),
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
        val errorDescription: String? = null,
    ) {
        fun toParamsMap(): Map<String, EventValue> {
            val validation = if (errorCode == null) Validation.SUCCESS.param else Validation.FAIL.param
            val code = errorCode ?: SUCCESS_CODE
            return buildMap {
                put(AnalyticsParam.DAPP_NAME, dAppName.asStringValue())
                put(AnalyticsParam.DAPP_URL, dAppUrl.asStringValue())
                put(AnalyticsParam.METHOD_NAME, methodName.asStringValue())
                put(AnalyticsParam.BLOCKCHAIN, blockchain.asStringValue())
                put(AnalyticsParam.VALIDATION, validation.asStringValue())
                put(AnalyticsParam.ERROR_CODE, code.asStringValue())
                if (errorDescription != null) {
                    put(AnalyticsParam.ERROR_DESCRIPTION, errorDescription.asStringValue())
                }
            }
        }
    }

    enum class Validation(val param: String) {
        SUCCESS("Success"),
        FAIL("Fail"),
    }

    private companion object {
        const val SUCCESS_CODE = "0"
    }
}