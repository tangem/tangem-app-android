package com.tangem.tap.common.analytics.events

/**
[REDACTED_AUTHOR]
 */
sealed class WalletConnect(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Wallet Connect", event, params) {

    class NewSessionEstablished : WalletConnect("New Session Established")
    class SessionDisconnected : WalletConnect("Session Disconnected")
    class RequestSigned : WalletConnect("Request Signed")
}