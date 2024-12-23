package com.tangem.tap.common.analytics.events

import com.tangem.core.analytics.models.AnalyticsEvent

/**
[REDACTED_AUTHOR]
 */
internal sealed class WalletConnect(
    event: String,
    params: Map<String, String> = mapOf(),
    error: Throwable? = null,
) : AnalyticsEvent("Wallet Connect", event, params, error) {

    class ScreenOpened : WalletConnect(event = "WC Screen Opened")
    class NewSessionEstablished(dAppName: String, dAppUrl: String, blockchainNames: List<String>) : WalletConnect(
        event = "New Session Established",
        params = mapOf(
            AnalyticsParam.DAPP_NAME to dAppName,
            AnalyticsParam.DAPP_URL to dAppUrl,
            AnalyticsParam.BLOCKCHAIN to blockchainNames.joinToString(","),
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
        val errorDescription: String? = null,
    ) {
        fun toParamsMap(): Map<String, String> {
            val validation = if (errorCode == null) Validation.SUCCESS.param else Validation.FAIL.param
            val code = errorCode ?: SUCCESS_CODE
            return buildMap {
                put(AnalyticsParam.DAPP_NAME, dAppName)
                put(AnalyticsParam.DAPP_URL, dAppUrl)
                put(AnalyticsParam.METHOD_NAME, methodName)
                put(AnalyticsParam.BLOCKCHAIN, blockchain)
                put(AnalyticsParam.VALIDATION, validation)
                put(AnalyticsParam.ERROR_CODE, code)
                if (errorDescription != null) {
                    put(AnalyticsParam.ERROR_DESCRIPTION, errorDescription)
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