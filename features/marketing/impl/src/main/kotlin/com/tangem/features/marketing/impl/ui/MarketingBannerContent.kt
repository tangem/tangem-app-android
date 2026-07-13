package com.tangem.features.marketing.impl.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.features.marketing.impl.ui.state.MarketingBannerListUM

@Composable
internal fun MarketingBannerContent(
    state: MarketingBannerListUM,
    onBannerClick: (String?) -> Unit,
    onDismiss: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is MarketingBannerListUM.Hidden -> Unit
        is MarketingBannerListUM.Content -> {
            val banners = state.banners
            if (banners.size == 1) {
                val banner = banners.first()
                MarketingBanner(
                    banner = banner,
                    onClick = { onBannerClick(banner.deeplink) },
                    onDismiss = { onDismiss(banner.campaignId) },
                    modifier = modifier,
                )
            } else {
                MarketingBannerCarousel(
                    banners = banners,
                    onBannerClick = onBannerClick,
                    onDismiss = onDismiss,
                    modifier = modifier,
                )
            }
        }
    }
}