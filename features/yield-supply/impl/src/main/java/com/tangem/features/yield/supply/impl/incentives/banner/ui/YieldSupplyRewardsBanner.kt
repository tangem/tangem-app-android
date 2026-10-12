package com.tangem.features.yield.supply.impl.incentives.banner.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.incentives.banner.entity.YieldSupplyRewardsBannerUM

@Composable
internal fun YieldSupplyRewardsBanner(
    state: YieldSupplyRewardsBannerUM,
    onClaimClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TangemMessageBanner(
        modifier = modifier,
        title = state.title,
        description = state.description,
        variant = TangemMessageBanner.Variant.Info,
        slotEnd = if (state.claim == YieldSupplyRewardsBannerUM.Claim.Hidden) {
            null
        } else {
            {
                TangemButton(
                    text = resourceReference(R.string.common_claim),
                    variant = TangemButton.Variant.Primary,
                    size = TangemButton.Size.X9,
                    isEnabled = state.claim == YieldSupplyRewardsBannerUM.Claim.Enabled,
                    onClick = onClaimClick,
                )
            }
        },
    )
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun YieldSupplyRewardsBannerPreview(
    @PreviewParameter(YieldSupplyRewardsBannerProvider::class) state: YieldSupplyRewardsBannerUM,
) {
    TangemThemePreviewRedesign {
        YieldSupplyRewardsBanner(state = state, onClaimClick = {})
    }
}

private class YieldSupplyRewardsBannerProvider : PreviewParameterProvider<YieldSupplyRewardsBannerUM> {
    override val values = sequenceOf(
        YieldSupplyRewardsBannerUM(
            title = resourceReference(R.string.yield_module_rewards_banner_title, wrappedList("\$30.00")),
            description = resourceReference(R.string.yield_module_rewards_banner_subtitle),
            claim = YieldSupplyRewardsBannerUM.Claim.Enabled,
        ),
        YieldSupplyRewardsBannerUM(
            title = resourceReference(R.string.yield_module_rewards_banner_title, wrappedList("\$0.30")),
            description = resourceReference(R.string.yield_module_rewards_banner_below_threshold_subtitle),
            claim = YieldSupplyRewardsBannerUM.Claim.Disabled,
        ),
        YieldSupplyRewardsBannerUM(
            title = resourceReference(R.string.yield_module_rewards_banner_pending_title),
            description = resourceReference(
                id = R.string.yield_module_rewards_banner_pending_subtitle,
                formatArgs = wrappedList("WETH 12.50"),
            ),
            claim = YieldSupplyRewardsBannerUM.Claim.Hidden,
        ),
    )
}