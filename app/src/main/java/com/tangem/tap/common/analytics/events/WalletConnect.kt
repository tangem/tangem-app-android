package com.tangem.tap.common.analytics.events

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction.OpenSession.SourceType

/**
[REDACTED_AUTHOR]
 */
internal sealed class WalletConnect(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Wallet Connect", event, params) {

    class ScreenOpened : WalletConnect(event = "WC Screen Opened")
    class NewSessionInitiated(source: SourceType) : WalletConnect(
        event = "Session Initiated",
        params = mapOf(
            AnalyticsParam.SOURCE to when (source) {
                SourceType.QR -> "QR"
                SourceType.DEEPLINK -> "DeepLink"
                SourceType.CLIPBOARD -> "Clipboard"
                SourceType.ETC -> "etc"
            },
        ),
    )

    data object SessionFailed : WalletConnect(
        event = "Session Failed",
    )

    class DAppConnectionRequested(
        blockchainNames: List<String>,
    ) : WalletConnect(
        event = "dApp Connection Requested",
        params = mapOf(
            AnalyticsParam.NETWORKS to blockchainNames.joinToString(","),
        ),
    )

    class DAppConnected(dAppName: String, dAppUrl: String, blockchainNames: List<String>) : WalletConnect(
        event = "dApp Connected",
        params = mapOf(
            AnalyticsParam.DAPP_NAME to dAppName,
            AnalyticsParam.DAPP_URL to dAppUrl,
            AnalyticsParam.BLOCKCHAIN to blockchainNames.joinToString(","),
        ),
    )

    class DAppConnectionFailed(dAppName: String, dAppUrl: String, blockchainNames: List<String>) : WalletConnect(
        event = "dApp Connection Failed",
        params = mapOf(
            AnalyticsParam.DAPP_NAME to dAppName,
            AnalyticsParam.DAPP_URL to dAppUrl,
            AnalyticsParam.BLOCKCHAIN to blockchainNames.joinToString(","),
        ),
    )

    class SessionDisconnected(dAppName: String, dAppUrl: String) : WalletConnect(
        event = "dApp Disconnected",
        params = mapOf(
            AnalyticsParam.DAPP_NAME to dAppName,
            AnalyticsParam.DAPP_URL to dAppUrl,
        ),
    )

    class SignatureRequestHandled(
        params: RequestHandledParams,
    ) : WalletConnect(
        event = "Signature Request Handled",
        params = params.toParamsMap(),
    )

    class SignatureRequestReceived(
        params: RequestHandledParams,
    ) : WalletConnect(
        event = "Signature Request Received",
        params = params.toParamsMap(),
    )

    class SignatureRequestFailed(
        params: RequestHandledParams,
    ) : WalletConnect(
        event = "Signature Request Failed",
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