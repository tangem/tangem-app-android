package com.tangem.features.marketing.impl.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.domain.marketing.models.MarketingBanner
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Immutable
internal sealed interface MarketingBannerListUM {

    data object Hidden : MarketingBannerListUM

    data class Content(val banners: ImmutableList<MarketingBannerUM>) : MarketingBannerListUM
}

internal fun MarketingBannerListUM.replacedSlots(): Set<MarketingBanner.RecommendedSlot> =
    (this as? MarketingBannerListUM.Content)?.banners?.mapNotNullTo(mutableSetOf()) { it.replacesSlot }.orEmpty()

internal fun MarketingBannerListUM.forSlot(slot: MarketingBanner.RecommendedSlot?): MarketingBannerListUM {
    if (this !is MarketingBannerListUM.Content) return this
    val matching = banners.filter { it.replacesSlot == slot }
        .let { if (slot == null) it else it.take(1) }
        .toImmutableList()
    return if (matching.isEmpty()) MarketingBannerListUM.Hidden else MarketingBannerListUM.Content(matching)
}