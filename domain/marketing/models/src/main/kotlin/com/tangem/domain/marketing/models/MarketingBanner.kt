package com.tangem.domain.marketing.models

data class MarketingBanner(
    val uiType: UiType,
    val text: String?,
    val iconUrl: String?,
    val iconAlign: IconAlign?,
    val bgColor: String?,
    val deeplink: String?,
    val isDismissible: Boolean,
    val categoryLabel: String? = null,
    val subtitle: String? = null,
    val etaLabel: String? = null,
    val partnerLabel: String? = null,
    val buttonText: String? = null,
    val hasGlow: Boolean = false,
) {

    enum class UiType { STANDALONE, LINKED_TO_PROVIDER, PSEUDO_PROVIDER, STANDALONE_CTA }

    enum class IconAlign { LEFT, RIGHT }
}