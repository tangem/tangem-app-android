package com.tangem.features.marketing.impl.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.messagebanner.CloseButton
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.marketing.impl.ui.state.MarketingBannerUM

/**
 * Marketing banner rendered with the design-system [TangemMessageBanner] (DS3): default variant with
 * the "magic" glow ring, a title, an optional icon slot, and a cross-circle dismiss button.
 *
 * The whole banner is clickable and launches [onClick] (its deeplink) — the marketing API exposes no
 * banner buttons, only a single deeplink. The API's `bgColor` is intentionally not applied here: the DS
 * component drives the background via its fixed [TangemMessageBanner.Variant], matching the design.
 */
@Composable
internal fun MarketingBanner(
    banner: MarketingBannerUM,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasDeeplink = !banner.deeplink.isNullOrBlank()
    // Hide the icon slot (and its gap) when the image fails to load, so a broken URL leaves no empty gap.
    var isIconFailed by remember(banner.iconUrl) { mutableStateOf(false) }
    val hasIcon = !banner.iconUrl.isNullOrBlank() && !isIconFailed
    val isIconAtStart = hasIcon && banner.iconAlign == MarketingBannerUM.IconAlign.LEFT
    val isIconAtEnd = hasIcon && banner.iconAlign == MarketingBannerUM.IconAlign.RIGHT

    TangemMessageBanner(
        title = stringReference(banner.text.orEmpty()),
        modifier = modifier,
        variant = TangemMessageBanner.Variant.Default,
        showGlowRing = false,
        onClick = if (hasDeeplink) onClick else null,
        slotStart = if (isIconAtStart) {
            { BannerIcon(banner.iconUrl, onLoadError = { isIconFailed = true }) }
        } else {
            null
        },
        slotEnd = if (isIconAtEnd || banner.isDismissible) {
            {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isIconAtEnd) {
                        BannerIcon(banner.iconUrl, onLoadError = { isIconFailed = true })
                    }
                    if (banner.isDismissible) {
                        TangemMessageBanner.CloseButton(
                            onClick = onDismiss,
                            contentDescription = stringResourceSafe(R.string.common_close),
                        )
                    }
                }
            }
        } else {
            null
        },
    )
}

@Composable
private fun BannerIcon(iconUrl: String?, onLoadError: () -> Unit) {
    if (iconUrl.isNullOrBlank()) return
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(iconUrl)
            .crossfade(true)
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        onError = { onLoadError() },
        modifier = Modifier.size(20.dp),
    )
}

// region Preview

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_MarketingBanner() {
    TangemThemePreviewRedesign {
        MarketingBanner(
            banner = MarketingBannerUM(
                campaignId = 1,
                text = "1:1 onramp at 0 fees!",
                iconUrl = null,
                iconAlign = MarketingBannerUM.IconAlign.LEFT,
                isDismissible = true,
                deeplink = "tangem://promo/1",
            ),
            onClick = {},
            onDismiss = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun Preview_MarketingBanner_NotDismissible() {
    TangemThemePreviewRedesign {
        MarketingBanner(
            banner = MarketingBannerUM(
                campaignId = 2,
                text = "Earn up to 14% APY by staking your crypto directly from the wallet",
                iconUrl = null,
                iconAlign = MarketingBannerUM.IconAlign.RIGHT,
                isDismissible = false,
                deeplink = null,
            ),
            onClick = {},
            onDismiss = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

// endregion