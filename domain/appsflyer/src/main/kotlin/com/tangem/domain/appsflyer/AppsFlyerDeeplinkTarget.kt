package com.tangem.domain.appsflyer

sealed interface AppsFlyerDeeplinkTarget {

    data class Known(val deeplink: AppsFlyerDeeplink) : AppsFlyerDeeplinkTarget

    data class TangemPayCampaign(val key: String) : AppsFlyerDeeplinkTarget

    companion object {

        private const val TANGEM_PAY_CAMPAIGN_PREFIX = "tpay_for_"

        fun from(deepLinkValue: String?): AppsFlyerDeeplinkTarget? {
            if (deepLinkValue == null) return null

            AppsFlyerDeeplink.from(deepLinkValue)?.let { return Known(it) }

            if (!deepLinkValue.startsWith(TANGEM_PAY_CAMPAIGN_PREFIX)) return null
            if (deepLinkValue.removePrefix(TANGEM_PAY_CAMPAIGN_PREFIX).isBlank()) return null

            return TangemPayCampaign(key = deepLinkValue)
        }
    }
}