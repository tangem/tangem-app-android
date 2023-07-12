package com.tangem.tap.common.analytics.events

import com.tangem.core.analytics.models.AnalyticsEvent

/**
* [REDACTED_AUTHOR]
 */
sealed class WalletConnect(
    event: String,
    params: Map<String, String> = mapOf(),
    error: Throwable? = null,
) : AnalyticsEvent("Wallet Connect", event, params, error) {

    class ScreenOpened : WalletConnect(event = "WC Screen Opened")
    class NewSessionEstablished(dAppName: String) : WalletConnect(
        event = "New Session Established",
        params = mapOf(
            AnalyticsParam.DAPP_NAME to dAppName,
        ),
    )

    class SessionDisconnected : WalletConnect("Session Disconnected")
    class RequestSigned : WalletConnect("Request Signed")

    class SignError(error: Throwable) : WalletConnect("Sign", error = error)
    class TransactionError(error: Throwable) : WalletConnect("Transaction", error = error)
}
