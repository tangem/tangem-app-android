package com.tangem.features.marketing.impl.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.ds2.glowring.TangemGlowRing
import com.tangem.core.ui.ds2.messagebanner.CloseButton
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.marketing.impl.ui.state.MarketingBannerUM

private val CORNER_RADIUS = 14.dp

/**
 * `standalone_cta` campaign strip: two lines of text, an optional icon and a full-width button.
 *
 * Unlike [MarketingBanner], only the button launches [onClick]; the strip body is not clickable.
 */
@Composable
internal fun StandaloneCtaMarketingBanner(
    banner: MarketingBannerUM,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = TangemTheme.colors3.bg.secondary, shape = RoundedCornerShape(CORNER_RADIUS))
                .padding(12.dp),
        ) {
            StandaloneCtaHeader(banner = banner, onDismiss = onDismiss)
            val buttonText = banner.buttonText
            if (!buttonText.isNullOrBlank()) {
                SpacerH(12.dp)
                PrimaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = buttonText,
                    onClick = onClick,
                )
            }
        }

        if (banner.hasGlow) {
            TangemGlowRing(
                modifier = Modifier.matchParentSize(),
                variant = TangemGlowRing.Variant.Magic,
                cornerRadius = CORNER_RADIUS,
            )
        }
    }
}

@Composable
private fun StandaloneCtaHeader(banner: MarketingBannerUM, onDismiss: () -> Unit) {
    // Drop the icon slot when the image fails to load, so a broken URL leaves no empty gap.
    var isIconFailed by remember(banner.iconUrl) { mutableStateOf(false) }
    val hasIcon = !banner.iconUrl.isNullOrBlank() && !isIconFailed
    val isIconAtStart = hasIcon && banner.iconAlign == MarketingBannerUM.IconAlign.LEFT
    val isIconAtEnd = hasIcon && banner.iconAlign == MarketingBannerUM.IconAlign.RIGHT

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isIconAtStart) {
            BannerIcon(banner.iconUrl, onLoadError = { isIconFailed = true })
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = banner.text.orEmpty(),
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors3.text.primary,
            )
            banner.subtitle?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = TangemTheme.typography.caption2,
                    color = TangemTheme.colors3.text.secondary,
                )
            }
        }

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

// region Preview

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_StandaloneCtaMarketingBanner(
    @PreviewParameter(StandaloneCtaMarketingBannerPreviewProvider::class) banner: MarketingBannerUM,
) {
    TangemThemePreviewRedesign {
        Box(
            modifier = Modifier
                .background(TangemTheme.colors.background.primary)
                .padding(16.dp),
        ) {
            StandaloneCtaMarketingBanner(banner = banner, onClick = {}, onDismiss = {})
        }
    }
}

private class StandaloneCtaMarketingBannerPreviewProvider : PreviewParameterProvider<MarketingBannerUM> {
    private val waitlist = MarketingBannerUM(
        campaignId = 1,
        text = "From bank to stablecoins 1:1",
        iconUrl = null,
        iconAlign = MarketingBannerUM.IconAlign.RIGHT,
        isDismissible = false,
        deeplink = "tangem://survey",
        subtitle = "USD and EUR, no on-ramp fees",
        buttonText = "Join waitlist",
        hasGlow = true,
    )

    override val values: Sequence<MarketingBannerUM>
        get() = sequenceOf(
            waitlist,
            waitlist.copy(isDismissible = true, hasGlow = false),
            waitlist.copy(subtitle = null, hasGlow = false),
            waitlist.copy(buttonText = null, hasGlow = false),
        )
}

// endregion