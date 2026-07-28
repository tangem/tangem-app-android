package com.tangem.features.marketing.impl.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.pager.PagerIndicator
import com.tangem.features.marketing.impl.ui.state.MarketingBannerUM
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun MarketingBannerCarousel(
    banners: ImmutableList<MarketingBannerUM>,
    onBannerClick: (String?) -> Unit,
    onDismiss: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { banners.size })

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            pageSpacing = 8.dp,
            key = { page -> banners[page].campaignId },
        ) { page ->
            val banner = banners[page]
            MarketingBanner(
                banner = banner,
                onClick = { onBannerClick(banner.deeplink) },
                onDismiss = { onDismiss(banner.campaignId) },
            )
        }
        PagerIndicator(pagerState = pagerState, hasBackground = false)
    }
}