package com.tangem.tap.common.analytics.events

/**
 * Created by Anton Zhilenkov on 28.09.2022.
 */
sealed class WalletConnect(
    event: String,
    params: Map<String, String> = mapOf(),
    error: Throwable? = null,
) : AnalyticsEvent("Wallet Connect", event, params, error) {

    class NewSessionEstablished : WalletConnect("New Session Established")
    class SessionDisconnected : WalletConnect("Session Disconnected")
    class RequestSigned : WalletConnect("Request Signed")

    class SignError(error: Throwable) : WalletConnect("Sign", error = error)
    class TransactionError(error: Throwable) : WalletConnect("Transaction", error = error)
}
