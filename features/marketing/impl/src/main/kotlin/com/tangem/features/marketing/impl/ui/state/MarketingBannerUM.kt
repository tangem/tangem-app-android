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
    val categoryLabel: String? = null,
    val subtitle: String? = null,
    val etaLabel: String? = null,
    val partnerLabel: String? = null,
    val buttonText: String? = null,
    val hasGlow: Boolean = false,
) {
    enum class IconAlign { LEFT, RIGHT }
}