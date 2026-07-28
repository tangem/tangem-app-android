package com.tangem.features.marketing.impl.ui.state

import androidx.compose.runtime.Immutable

@Immutable
internal data class MarketingBannerUM(
    val campaignId: Int,
    val text: String?,
    val iconUrl: String?,
    val iconAlign: IconAlign,
    val isDismissible: Boolean,
    val deeplink: String?,
    val providerIds: Set<String> = emptySet(),
) {
    enum class IconAlign { LEFT, RIGHT }
}