package com.tangem.features.promobanners.impl.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.tangem.core.ui.components.SpacerH8
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.components.pager.PagerIndicator
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.messagebanner.CloseButton
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.extensions.orEmpty
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.promobanners.api.PromoBannersBlockComponent.Placeholder
import com.tangem.features.promobanners.impl.model.PromoBannerNotificationUM
import com.tangem.features.promobanners.impl.model.PromoBannersBlockUM
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.ceil
import kotlin.math.floor

@Composable
internal fun PromoBannersBlock(state: PromoBannersBlockUM, horizontalItemPadding: Dp, modifier: Modifier = Modifier) {
    if (state.banners.isEmpty()) return

    if (state.banners.size == 1) {
        val banner = state.banners.first()
        LaunchedEffect(banner.displayId, state.isVisibleOnScreen) {
            if (state.isVisibleOnScreen) {
                state.onBannerShown(banner.displayId)
            }
        }
        SingleBanner(
            banner = banner,
            modifier = modifier.padding(horizontal = horizontalItemPadding),
        )
    } else {
        key(state.userWalletId) {
            BannersCarousel(
                state = state,
                horizontalPadding = horizontalItemPadding,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun BannerNotification(config: NotificationConfig, modifier: Modifier = Modifier) {
    TangemMessageBanner(
        modifier = modifier.fillMaxWidth(),
        title = config.title.orEmpty(),
        description = config.subtitle,
        variant = TangemMessageBanner.Variant.Solid,
        showGlowRing = false,
        secondaryButton = config.buttonsState.toSecondaryButton(),
        primaryButton = config.buttonsState.toPrimaryButton(),
        slotStart = {
            TangemIcon(
                tangemIconUM = TangemIconUM.Url(url = config.iconUrl, fallbackRes = config.iconResId),
                modifier = Modifier.size(config.iconSize),
            )
        },
        slotEnd = config.onCloseClick?.let { onCloseClick ->
            { TangemMessageBanner.CloseButton(onClick = onCloseClick) }
        },
    )
}

/** Maps a [NotificationConfig.ButtonsState.SecondaryButtonConfig] to the banner's secondary action. */
private fun NotificationConfig.ButtonsState?.toSecondaryButton(): TangemMessageBanner.Button? = when (this) {
    is NotificationConfig.ButtonsState.SecondaryButtonConfig -> TangemMessageBanner.Button(
        text = text,
        onClick = onClick,
        isLoading = shouldShowProgress,
        iconStart = iconResId?.let { TangemIconUM.Icon(iconRes = it) },
    )
    else -> null
}

/** Maps a [NotificationConfig.ButtonsState.PrimaryButtonConfig] to the banner's primary action. */
private fun NotificationConfig.ButtonsState?.toPrimaryButton(): TangemMessageBanner.Button? = when (this) {
    is NotificationConfig.ButtonsState.PrimaryButtonConfig -> TangemMessageBanner.Button(
        text = text,
        onClick = onClick,
        isLoading = shouldShowProgress,
        iconStart = iconResId?.let { TangemIconUM.Icon(iconRes = it) },
    )
    else -> null
}

@Composable
private fun SingleBanner(banner: PromoBannerNotificationUM, modifier: Modifier = Modifier) {
    BannerNotification(
        config = banner.config,
        modifier = modifier,
    )
}

@Composable
private fun BannersCarousel(state: PromoBannersBlockUM, horizontalPadding: Dp, modifier: Modifier = Modifier) {
    val pagerState = rememberPagerState(
        initialPage = state.initialPage,
        pageCount = { state.banners.size },
    )

    LaunchedEffect(pagerState, state.banners, state.isVisibleOnScreen) {
        if (!state.isVisibleOnScreen) return@LaunchedEffect
        var previousPage = pagerState.currentPage
        snapshotFlow { pagerState.currentPage }
            .collect { page ->
                state.banners.getOrNull(page)?.let { banner ->
                    state.onPageChanged(banner.displayId)
                    state.onBannerShown(banner.displayId)
                    if (previousPage == 0 && page == 1) {
                        state.onCarouselScrolled(banner.displayId)
                    }
                }
                previousPage = page
            }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SmoothHeightPager(
            banners = state.banners,
            pagerState = pagerState,
            horizontalPadding = horizontalPadding,
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
    horizontalPadding: Dp,
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
            BannerNotification(
                config = banners[lowerPage].config,
                modifier = Modifier.padding(horizontal = horizontalPadding),
            )
        }.first().measure(pageConstraints).height

        val upperHeight = if (upperPage != lowerPage) {
            subcompose(slotId = "measure_upper") {
                BannerNotification(
                    config = banners[upperPage].config,
                    modifier = Modifier.padding(horizontal = horizontalPadding),
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
                BannerNotification(
                    modifier = Modifier.padding(horizontal = horizontalPadding),
                    config = banner.config,
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

// region Preview
private fun previewState(bannerCount: Int) = PromoBannersBlockUM(
    userWalletId = "preview",
    initialPage = 0,
    banners = persistentListOf(
        *Array(bannerCount) { index ->
            PromoBannerNotificationUM(
                displayId = index,
                config = NotificationConfig(
                    title = stringReference("Earn up to 14% APY"),
                    subtitle = stringReference("Staking is the easiest way to earn rewards on your crypto."),
                    iconResId = com.tangem.core.ui.R.drawable.ic_alert_circle_24,
                    buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                        text = stringReference("Start earning"),
                        onClick = {},
                    ),
                    onCloseClick = {},
                ),
            )
        },
    ),
    isVisibleOnScreen = false,
    placeholder = Placeholder.MAIN,
    onBannerShown = {},
    onCarouselScrolled = {},
    onPageChanged = {},
)

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_PromoBannersBlock_Redesign() {
    TangemThemePreviewRedesign {
        PromoBannersBlock(
            state = previewState(bannerCount = 2),
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalItemPadding = 12.dp,
        )
    }
}
// endregion