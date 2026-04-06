package com.tangem.features.promobanners.impl.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.util.lerp
import com.tangem.core.ui.components.SpacerH8
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.components.pager.PagerIndicator
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.promobanners.impl.model.PromoBannerNotificationUM
import com.tangem.features.promobanners.impl.model.PromoBannersBlockUM
import kotlin.math.ceil
import kotlin.math.floor

@Composable
internal fun PromoBannersBlock(state: PromoBannersBlockUM, modifier: Modifier = Modifier) {
    if (state.banners.isEmpty()) return

    if (state.banners.size == 1) {
        val banner = state.banners.first()
        LaunchedEffect(banner.displayId) {
            state.onBannerShown(banner.displayId)
        }
        SingleBanner(
            banner = banner,
            modifier = modifier,
        )
    } else {
        BannersCarousel(
            banners = state.banners,
            onBannerShown = state.onBannerShown,
            onCarouselScroll = state.onCarouselScrolled,
            modifier = modifier,
        )
    }
}

@Composable
private fun SingleBanner(banner: PromoBannerNotificationUM, modifier: Modifier = Modifier) {
    Notification(
        config = banner.config,
        modifier = modifier.fillMaxWidth(),
        containerColor = TangemTheme.colors.background.primary,
    )
}

@Composable
private fun BannersCarousel(
    banners: List<PromoBannerNotificationUM>,
    onBannerShown: (String) -> Unit,
    onCarouselScroll: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { banners.size })

    LaunchedEffect(pagerState, banners) {
        snapshotFlow { pagerState.currentPage }
            .collect { page ->
                banners.getOrNull(page)?.let { banner ->
                    onBannerShown(banner.displayId)
                    if (page == 1) {
                        // one-time event when user scrolls from first to second page,
                        // indicating that he has interacted with the carousel
                        onCarouselScroll(banner.displayId)
                    }
                }
            }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SmoothHeightPager(
            banners = banners,
            pagerState = pagerState,
            modifier = Modifier.fillMaxWidth(),
        )

        SpacerH8()

        PagerIndicator(
            pagerState = pagerState,
            hasBackground = false,
        )
    }
}

/**
 * HorizontalPager that smoothly interpolates its height between pages
 * based on the current scroll position, preventing layout jumps.
 *
 * Only the two adjacent pages (lower and upper of the current scroll position)
 * are measured per frame — O(1) instead of O(N).
 */
@Composable
private fun SmoothHeightPager(
    banners: List<PromoBannerNotificationUM>,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        if (banners.isEmpty()) {
            return@SubcomposeLayout layout(constraints.maxWidth, 0) {}
        }

        val pageConstraints = constraints.copy(minHeight = 0)

        val scrollPosition = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
            .coerceIn(0f, banners.lastIndex.toFloat())
        val lowerPage = floor(scrollPosition).toInt().coerceIn(banners.indices)
        val upperPage = ceil(scrollPosition).toInt().coerceIn(banners.indices)
        val fraction = scrollPosition - floor(scrollPosition)

        val lowerHeight = subcompose(slotId = "measure_lower") {
            Notification(
                config = banners[lowerPage].config,
                modifier = Modifier.fillMaxWidth(),
                containerColor = TangemTheme.colors.background.primary,
            )
        }.first().measure(pageConstraints).height

        val upperHeight = if (upperPage != lowerPage) {
            subcompose(slotId = "measure_upper") {
                Notification(
                    config = banners[upperPage].config,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = TangemTheme.colors.background.primary,
                )
            }.first().measure(pageConstraints).height
        } else {
            lowerHeight
        }

        val interpolatedHeight = lerp(lowerHeight, upperHeight, fraction)

        val pagerPlaceable = subcompose(slotId = "pager") {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                pageSpacing = TangemTheme.dimens.spacing16,
            ) { page ->
                val banner = banners.getOrNull(page) ?: return@HorizontalPager
                Notification(
                    config = banner.config,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = TangemTheme.colors.background.primary,
                )
            }
        }.first().measure(
            constraints.copy(minHeight = interpolatedHeight, maxHeight = interpolatedHeight),
        )

        layout(pagerPlaceable.width, interpolatedHeight) {
            pagerPlaceable.place(0, 0)
        }
    }
}