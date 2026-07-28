package com.tangem.domain.appsflyer

/** Known AppsFlyer navigational deep links, keyed by their `deep_link_value`. */
enum class AppsFlyerDeeplink(val deepLinkValue: String) {
    TangemPayMobileOnboarding(deepLinkValue = "tpay_mobileonboard"),
    Referral(deepLinkValue = "referral"),
    ;

    companion object {
        fun from(deepLinkValue: String?): AppsFlyerDeeplink? = entries.firstOrNull { it.deepLinkValue == deepLinkValue }
    }
}