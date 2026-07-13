package com.tangem.features.marketing.impl.ui.state

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed interface MarketingBannerListUM {

    data object Hidden : MarketingBannerListUM

    data class Content(val banners: ImmutableList<MarketingBannerUM>) : MarketingBannerListUM
}