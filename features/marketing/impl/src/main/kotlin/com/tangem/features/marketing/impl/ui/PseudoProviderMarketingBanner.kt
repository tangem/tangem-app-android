package com.tangem.features.marketing.impl.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.components.buttons.common.TangemButtonSize
import com.tangem.core.ui.ds2.glowring.TangemGlowRing
import com.tangem.core.ui.ds2.messagebanner.CloseButton
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.marketing.impl.ui.state.MarketingBannerUM

private val CORNER_RADIUS = 14.dp

/**
 * `pseudo_provider` campaign card, laid out like an onramp offer row so it reads as one more way to buy.
 *
 * Only the button launches [onClick]; the card body is intentionally not clickable.
 */
@Composable
internal fun PseudoProviderMarketingBanner(
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
            PseudoProviderHeader(banner = banner, onClick = onClick, onDismiss = onDismiss)

            if (banner.etaLabel != null || banner.partnerLabel != null) {
                SpacerH(10.dp)
                HorizontalDivider(color = TangemTheme.colors3.border.secondary)
                SpacerH(12.dp)
                PseudoProviderFooter(etaLabel = banner.etaLabel, partnerLabel = banner.partnerLabel)
            }
        }

        if (banner.hasGlow) {
            TangemGlowRing(
                modifier = Modifier.matchParentSize(),
                variant = TangemGlowRing.Variant.Success,
                cornerRadius = CORNER_RADIUS,
            )
        }
    }
}

@Composable
private fun PseudoProviderHeader(banner: MarketingBannerUM, onClick: () -> Unit, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            banner.categoryLabel?.let { categoryLabel ->
                Text(
                    text = categoryLabel,
                    style = TangemTheme.typography.caption1,
                    color = if (banner.hasGlow) {
                        TangemTheme.colors3.text.accent.green
                    } else {
                        TangemTheme.colors3.text.accent.violet
                    },
                )
            }
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

        val buttonText = banner.buttonText
        if (!buttonText.isNullOrBlank()) {
            SpacerW(8.dp)
            PrimaryButton(
                size = TangemButtonSize.RoundedAction,
                text = buttonText,
                onClick = onClick,
            )
        }

        if (banner.isDismissible) {
            SpacerW(8.dp)
            TangemMessageBanner.CloseButton(
                onClick = onDismiss,
                contentDescription = stringResourceSafe(R.string.common_close),
            )
        }
    }
}

@Composable
private fun PseudoProviderFooter(etaLabel: String?, partnerLabel: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (etaLabel != null) {
            Icon(
                modifier = Modifier.size(16.dp),
                imageVector = ImageVector.vectorResource(R.drawable.ic_clock_24),
                tint = TangemTheme.colors3.icon.secondary,
                contentDescription = null,
            )
            SpacerW(2.dp)
            FooterText(text = etaLabel)
        }

        SpacerWMax()

        if (partnerLabel != null) {
            FooterText(text = partnerLabel)
        }
    }
}

@Composable
private fun FooterText(text: String) {
    Text(
        text = text,
        style = TangemTheme.typography.caption2,
        color = TangemTheme.colors3.text.secondary,
    )
}

// region Preview

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_PseudoProviderMarketingBanner(
    @PreviewParameter(PseudoProviderMarketingBannerPreviewProvider::class) banner: MarketingBannerUM,
) {
    TangemThemePreviewRedesign {
        Box(
            modifier = Modifier
                .background(TangemTheme.colors.background.primary)
                .padding(16.dp),
        ) {
            PseudoProviderMarketingBanner(banner = banner, onClick = {}, onDismiss = {})
        }
    }
}

private class PseudoProviderMarketingBannerPreviewProvider : PreviewParameterProvider<MarketingBannerUM> {
    private val concierge = MarketingBannerUM(
        campaignId = 1,
        text = "Personal support manager",
        iconUrl = null,
        iconAlign = MarketingBannerUM.IconAlign.LEFT,
        isDismissible = false,
        deeplink = "tangem://support",
        categoryLabel = "Concierge for large purchases",
        subtitle = "Pay by bank transfer",
        etaLabel = "up to 4 hours",
        partnerLabel = "via MoonPay",
        buttonText = "Request",
        hasGlow = true,
    )

    override val values: Sequence<MarketingBannerUM>
        get() = sequenceOf(
            concierge,
            concierge.copy(
                text = "Buy with BTC Direct",
                categoryLabel = "OTC partner service",
                etaLabel = "up to 3 days",
                partnerLabel = "via BTC Direct",
                isDismissible = true,
                hasGlow = false,
            ),
            concierge.copy(buttonText = null, hasGlow = false),
            concierge.copy(
                categoryLabel = null,
                subtitle = null,
                etaLabel = null,
                partnerLabel = null,
                hasGlow = false,
            ),
        )
}

// endregion