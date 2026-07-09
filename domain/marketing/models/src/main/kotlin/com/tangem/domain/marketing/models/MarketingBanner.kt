package com.tangem.domain.marketing.models

data class MarketingBanner(
    val uiType: UiType,
    val text: String?,
    val iconUrl: String?,
    val iconAlign: IconAlign?,
    val bgColor: String?,
    val deeplink: String?,
    val isDismissible: Boolean,
) {

    enum class UiType { STANDALONE, LINKED_TO_PROVIDER }

    enum class IconAlign { LEFT, RIGHT }
}