package com.tangem.lib.auth.performance

/**
 * Flow that requested a nonce. Both wallet types hit the same wallet-nonce endpoint and share its
 * performance trace, but are reported separately in analytics.
 */
enum class NonceRequestType(val analyticsValue: String, val traceName: String) {
    DEVICE(analyticsValue = "Device", traceName = "device_nonce_request"),
    AUTHENTICATION(analyticsValue = "Authentication", traceName = "auth_nonce_request"),
    COLD_WALLET(analyticsValue = "Cold Wallet", traceName = "wallet_nonce_request"),
    MOBILE_WALLET(analyticsValue = "Mobile Wallet", traceName = "wallet_nonce_request"),
}