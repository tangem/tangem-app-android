package com.tangem.features.earn.impl.banner.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_right_20
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.api.overview.EarnRateType
import com.tangem.features.earn.impl.banner.EarnBannerTexts
import com.tangem.features.earn.impl.banner.ui.state.EarnBannerUM
import java.math.BigDecimal

@Composable
internal fun EarnBannerContent(state: EarnBannerUM, modifier: Modifier = Modifier) {
    when (state) {
        EarnBannerUM.Hidden -> Unit
        is EarnBannerUM.Offer -> TangemMessageBanner(
            title = state.title,
            modifier = modifier,
            variant = TangemMessageBanner.Variant.Info,
            onClick = state.onEnableClick,
            description = state.description,
            slotStart = { EarnBannerIcon() },
            slotEnd = {
                CenteredAgainstIcon {
                    TangemButton(
                        text = state.buttonText,
                        variant = TangemButton.Variant.Primary,
                        size = TangemButton.Size.X8,
                        onClick = state.onEnableClick,
                    )
                }
            },
        )
        is EarnBannerUM.Enabled -> TangemMessageBanner(
            title = state.title,
            modifier = modifier,
            variant = TangemMessageBanner.Variant.Info,
            onClick = state.onClick,
            description = state.description,
            slotStart = { EarnBannerIcon() },
            slotEnd = {
                CenteredAgainstIcon {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        imageVector = Icons.ic_chevron_right_20,
                        contentDescription = null,
                        tint = TangemTheme.colors3.icon.tertiary,
                    )
                }
            },
        )
    }
}

// The DS banner top-aligns slotEnd, while the design centres it against the icon-tall row
@Composable
private fun CenteredAgainstIcon(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.height(EARN_BANNER_ICON_SIZE),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

// region Preview
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnBannerContentPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            EarnBannerContent(
                state = EarnBannerUM.Offer(
                    title = EarnBannerTexts.title,
                    description = EarnBannerTexts.upTo(
                        rate = EarnProductRate(fraction = BigDecimal("0.055"), type = EarnRateType.APY),
                    ),
                    buttonText = EarnBannerTexts.enable,
                    onEnableClick = {},
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            EarnBannerContent(
                state = EarnBannerUM.Enabled(
                    title = EarnBannerTexts.enabledTitle,
                    description = EarnBannerTexts.enabledDescription(amount = "1.3254 ETH", activeCount = 1),
                    onClick = {},
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
// endregion