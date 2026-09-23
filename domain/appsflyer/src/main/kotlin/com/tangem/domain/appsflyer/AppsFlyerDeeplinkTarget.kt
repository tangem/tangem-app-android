package com.tangem.domain.appsflyer

sealed interface AppsFlyerDeeplinkTarget {

    data class Known(val deeplink: AppsFlyerDeeplink) : AppsFlyerDeeplinkTarget

    data class TangemPayCampaign(val key: String) : AppsFlyerDeeplinkTarget

    data class Direct(val uri: String) : AppsFlyerDeeplinkTarget

    companion object {

        private const val TANGEM_PAY_CAMPAIGN_PREFIX = "tpay_for_"
        private val DIRECT_URI_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://\\S+$")

        fun from(deepLinkValue: String?): AppsFlyerDeeplinkTarget? {
            if (deepLinkValue == null) return null

            AppsFlyerDeeplink.from(deepLinkValue)?.let { return Known(it) }

            if (DIRECT_URI_REGEX.matches(deepLinkValue)) return Direct(uri = deepLinkValue)

            if (!deepLinkValue.startsWith(TANGEM_PAY_CAMPAIGN_PREFIX)) return null
            if (deepLinkValue.removePrefix(TANGEM_PAY_CAMPAIGN_PREFIX).isBlank()) return null

            return TangemPayCampaign(key = deepLinkValue)
        }
    }
}