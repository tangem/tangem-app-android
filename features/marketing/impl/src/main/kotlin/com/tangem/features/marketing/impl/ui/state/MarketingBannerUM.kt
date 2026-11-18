package com.tangem.features.marketing.impl.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.marketing.models.MarketingBanner

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
    val buttonText: TextReference? = null,
    val hasGlow: Boolean = false,
    val replacesSlot: MarketingBanner.RecommendedSlot? = null,
    val style: Style = Style.DEFAULT,
) {
    enum class IconAlign { LEFT, RIGHT }

    enum class Style { DEFAULT, PSEUDO_PROVIDER, STANDALONE_CTA }
}