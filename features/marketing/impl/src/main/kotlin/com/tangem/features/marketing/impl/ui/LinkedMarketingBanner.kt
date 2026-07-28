package com.tangem.features.marketing.impl.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.marketing.impl.ui.state.MarketingBannerUM

private val BOTTOM_CORNER_RADIUS = 20.dp
private val ICON_SIZE = 16.dp

// DS3 has no dedicated "blue 10%" background token; derive it from the accent blue to match Figma
// (rgba(0,153,255,0.1)).
private const val BACKGROUND_ALPHA = 0.1f

/**
 * LINKED_TO_PROVIDER marketing banner — a compact accent strip glued to the bottom of an onramp provider
 * offer. Distinct from the standalone [MarketingBanner]: blue accent background, bottom-only rounded
 * corners, a 16dp icon and blue title, no dismiss button.
 *
 * [Figma](https://www.figma.com/design/GhMZiR8xGeGSmaLinuE5qq/Onramp?node-id=401-84213&m=dev)
 */
@Composable
internal fun LinkedMarketingBanner(banner: MarketingBannerUM, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val hasDeeplink = !banner.deeplink.isNullOrBlank()
    // Collapse the icon slot when the image fails to load, so a broken URL leaves no empty gap.
    var isIconFailed by remember(banner.iconUrl) { mutableStateOf(false) }
    val hasIcon = !banner.iconUrl.isNullOrBlank() && !isIconFailed

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(bottomStart = BOTTOM_CORNER_RADIUS, bottomEnd = BOTTOM_CORNER_RADIUS))
            .then(if (hasDeeplink) Modifier.clickableSingle(onClick = onClick) else Modifier)
            .background(TangemTheme.colors3.bg.accent.blue.copy(alpha = BACKGROUND_ALPHA))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (hasIcon) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(banner.iconUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(TangemTheme.colors3.icon.accent.blue),
                onError = { isIconFailed = true },
                modifier = Modifier.size(ICON_SIZE),
            )
        }
        Text(
            text = banner.text.orEmpty(),
            style = TangemTheme.typography2.subheadlineMedium14,
            color = TangemTheme.colors3.text.accent.blue,
        )
    }
}

// region Preview

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_LinkedMarketingBanner() {
    TangemThemePreviewRedesign {
        LinkedMarketingBanner(
            banner = MarketingBannerUM(
                campaignId = 1,
                text = "1:1 onramp at 0 fees!",
                iconUrl = null,
                iconAlign = MarketingBannerUM.IconAlign.LEFT,
                isDismissible = false,
                deeplink = "tangem://buy",
                providerIds = setOf("mercuryo"),
            ),
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

// endregion