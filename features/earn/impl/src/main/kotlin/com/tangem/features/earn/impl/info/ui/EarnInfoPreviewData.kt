package com.tangem.features.earn.impl.info.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.ImageReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_refresh_20
import com.tangem.core.ui.res.generated.icons.ic_lightning_20
import com.tangem.features.earn.api.ui.EarnRowUM
import com.tangem.features.earn.impl.info.EarnInfoTexts
import com.tangem.features.earn.impl.info.ui.state.EarnInfoTab
import com.tangem.features.earn.impl.info.ui.state.EarnInfoUM
import com.tangem.features.earn.impl.picker.ui.state.EarnBenefitUM
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

internal object EarnInfoPreviewData {

    fun full(): EarnInfoUM = EarnInfoUM(
        title = stringReference("Lending on Aave"),
        subtitle = stringReference("Ethereum in Mainnet"),
        balanceFiat = stringReference("\$5,200.52"),
        balanceCrypto = stringReference("ETH 1.482"),
        earned = EarnInfoTexts.earnedChip(amount = "\$2,150.32"),
        rewards = null,
        staleNotice = null,
        tabs = EarnInfoTab.entries.toImmutableList(),
        myFunds = EarnInfoUM.MyFundsUM(
            provider = stringReference("Aave"),
            providerIcon = ImageReference.Res(R.drawable.ic_aave_36),
            description = stringReference(
                "Your ETH is supplied to Aave and is earning interest. Your balance grows automatically, with no " +
                    "action needed. Withdraw anytime, subject to available Aave liquidity.",
            ),
            rows = persistentListOf(
                row(label = "Total balance", value = "ETH 1.482", hasInfo = true),
                row(label = "Available balance", value = "ETH 1.482", hasInfo = true),
            ),
            onHowItWorksClick = {},
        ),
        benefits = persistentListOf(
            EarnBenefitUM(
                icon = TangemIconUM.Icon(Icons.ic_lightning_20),
                title = stringReference("No lock-ups."),
                description = stringReference("Send, swap, or sell your funds instantly, anytime you want"),
            ),
            EarnBenefitUM(
                icon = TangemIconUM.Icon(Icons.ic_arrow_refresh_20),
                title = stringReference("Withdraw anytime."),
                description = stringReference("Aave's deep liquidity means your ETH is available on demand"),
            ),
        ),
        details = persistentListOf(
            row(label = "Risk", value = "Low", hasInfo = true),
            row(label = "Protocol", value = "Aave", isLink = true),
            row(label = "Withdrawals", value = "Usually instant", hasInfo = true),
            row(label = "Performance fee", value = "15% of yield", hasInfo = true),
        ),
        secondary = persistentListOf(button(text = "Withdraw", type = TangemButtonType.Secondary)),
        primary = button(text = "Add funds", type = TangemButtonType.Primary),
        onClose = {},
    )

    fun withoutBenefitsAndDetails(): EarnInfoUM = full().copy(
        earned = null,
        tabs = persistentListOf(EarnInfoTab.MyFunds),
        benefits = persistentListOf(),
        details = persistentListOf(),
    )

    fun stale(): EarnInfoUM = full().copy(
        staleNotice = EarnInfoTexts.staleNotice(asOf = "Nov 9, 2026, 9:41 AM"),
        secondary = persistentListOf(button(text = "Withdraw", type = TangemButtonType.Secondary, isDimmed = true)),
        primary = button(text = "Add funds", type = TangemButtonType.Primary, isDimmed = true),
    )

    fun withRewards(): EarnInfoUM = full().copy(
        rewards = EarnInfoUM.RewardsUM(
            title = EarnInfoTexts.rewardsTitle(amount = "\$30"),
            description = EarnInfoTexts.rewardsDescription,
            claimLabel = stringReference("Claim"),
            onClaimClick = {},
        ),
    )

    private fun row(label: String, value: String, hasInfo: Boolean = false, isLink: Boolean = false): EarnRowUM {
        val noop: () -> Unit = {}

        return EarnRowUM(
            label = stringReference(label),
            value = stringReference(value),
            onInfoClick = noop.takeIf { hasInfo },
            onValueClick = noop.takeIf { isLink },
        )
    }

    private fun button(text: String, type: TangemButtonType, isDimmed: Boolean = false) = TangemButtonUM(
        text = stringReference(text),
        isDimmed = isDimmed,
        type = type,
        onClick = {},
    )
}

@Composable
private fun ChartPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(CHART_PLACEHOLDER_HEIGHT)
            .background(color = TangemTheme.colors3.bg.secondary, shape = RoundedCornerShape(24.dp)),
    )
}

@Composable
private fun PreviewContent(state: EarnInfoUM, chart: (@Composable () -> Unit)? = { ChartPlaceholder() }) {
    TangemThemePreviewRedesign {
        EarnInfoContent(state = state, chart = chart)
    }
}

private val CHART_PLACEHOLDER_HEIGHT = 240.dp

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnInfoContentFullPreview() {
    PreviewContent(state = EarnInfoPreviewData.full())
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnInfoContentWithoutBenefitsAndDetailsPreview() {
    PreviewContent(state = EarnInfoPreviewData.withoutBenefitsAndDetails(), chart = null)
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnInfoContentStalePreview() {
    PreviewContent(state = EarnInfoPreviewData.stale())
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnInfoContentRewardsPreview() {
    PreviewContent(state = EarnInfoPreviewData.withRewards())
}