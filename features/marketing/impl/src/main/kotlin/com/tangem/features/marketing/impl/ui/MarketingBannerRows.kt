package com.tangem.features.marketing.impl.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.marketing.impl.ui.state.MarketingBannerListUM
import com.tangem.features.marketing.impl.ui.state.MarketingBannerUM
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun CtaMarketingBannerRows(
    state: MarketingBannerListUM,
    onBannerClick: (String?) -> Unit,
    onDismiss: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state !is MarketingBannerListUM.Content) return

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.banners.forEach { banner ->
            key(banner.campaignId) {
                val onClick = { onBannerClick(banner.deeplink) }
                val onBannerDismiss = { onDismiss(banner.campaignId) }
                when (banner.style) {
                    MarketingBannerUM.Style.PSEUDO_PROVIDER ->
                        PseudoProviderMarketingBanner(banner = banner, onClick = onClick, onDismiss = onBannerDismiss)
                    MarketingBannerUM.Style.STANDALONE_CTA ->
                        StandaloneCtaMarketingBanner(banner = banner, onClick = onClick, onDismiss = onBannerDismiss)
                    MarketingBannerUM.Style.DEFAULT -> Unit
                }
            }
        }
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_CtaMarketingBannerRows() {
    val concierge = MarketingBannerUM(
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
        style = MarketingBannerUM.Style.PSEUDO_PROVIDER,
    )
    val waitlist = MarketingBannerUM(
        campaignId = 2,
        text = "From bank to stablecoins 1:1",
        iconUrl = null,
        iconAlign = MarketingBannerUM.IconAlign.RIGHT,
        isDismissible = false,
        deeplink = "tangem://onramp?type=concierge_waitlist",
        subtitle = "USD and EUR, no on-ramp fees",
        buttonText = "Join waitlist",
        style = MarketingBannerUM.Style.STANDALONE_CTA,
    )

    TangemThemePreviewRedesign {
        Box(
            modifier = Modifier
                .background(TangemTheme.colors.background.primary)
                .padding(16.dp),
        ) {
            CtaMarketingBannerRows(
                state = MarketingBannerListUM.Content(persistentListOf(concierge, waitlist)),
                onBannerClick = {},
                onDismiss = {},
            )
        }
    }
}
// endregion