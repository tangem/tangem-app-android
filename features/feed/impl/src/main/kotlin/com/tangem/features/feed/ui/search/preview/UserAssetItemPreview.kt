package com.tangem.features.feed.ui.search.preview

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.marketprice.PriceChangeState
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.feed.ui.search.components.GroupedUserAssetItem
import com.tangem.features.feed.ui.search.components.SingleUserAssetItem
import com.tangem.features.feed.ui.search.state.BalanceDisplayState
import com.tangem.features.feed.ui.search.state.UserAssetItemUM

/** Labeled UI state for [SingleUserAssetItem] previews (dropdown label in Studio). */
internal data class SingleUserAssetItemPreviewScenario(
    val title: String,
    val item: UserAssetItemUM.Single,
)

/** Labeled UI state for [GroupedUserAssetItem] previews. */
internal data class GroupedUserAssetItemPreviewScenario(
    val title: String,
    val item: UserAssetItemUM.Grouped,
)

@Suppress("StringLiteralDuplication")
internal object UserAssetItemPreviewFixtures {

    private val sampleIcon = TangemIconUM.Currency(
        CurrencyIconState.CoinIcon(
            url = null,
            fallbackResId = com.tangem.core.ui.R.drawable.ic_ethereumpow_22,
            isGrayscale = false,
            shouldShowCustomBadge = false,
        ),
    )

    private val cryptoRef = stringReference("1.234 ETH")
    private val fiatRef = stringReference("$121,876.50")

    private val samplePriceChange = PriceChangeState.Content(
        valueInPercent = "+2.34%",
        type = PriceChangeType.UP,
    )

    private fun balanceLoaded() = BalanceDisplayState.Loaded(
        cryptoBalance = cryptoRef,
        fiatBalance = fiatRef,
    )

    private fun balanceFlickering() = BalanceDisplayState.Flickering(
        cryptoBalance = cryptoRef,
        fiatBalance = fiatRef,
    )

    private fun balanceStale() = BalanceDisplayState.Stale(
        cryptoBalance = cryptoRef,
        fiatBalance = fiatRef,
    )

    private fun balanceLoading() = BalanceDisplayState.Loading

    private fun balanceUnreachable() = BalanceDisplayState.Unreachable

    fun allSingleScenarios(): List<SingleUserAssetItemPreviewScenario> = listOf(
        SingleUserAssetItemPreviewScenario(
            title = "Hidden (balanceState ignored)",
            item = single(
                balanceState = balanceLoaded(),
                isBalanceHidden = true,
            ),
        ),
        SingleUserAssetItemPreviewScenario(
            title = "Balance – Loaded",
            item = single(balanceState = balanceLoaded(), isBalanceHidden = false),
        ),
        SingleUserAssetItemPreviewScenario(
            title = "Balance – Flickering",
            item = single(balanceState = balanceFlickering(), isBalanceHidden = false),
        ),
        SingleUserAssetItemPreviewScenario(
            title = "Balance – Stale",
            item = single(balanceState = balanceStale(), isBalanceHidden = false),
        ),
        SingleUserAssetItemPreviewScenario(
            title = "Balance – Loading",
            item = single(balanceState = balanceLoading(), isBalanceHidden = false),
        ),
        SingleUserAssetItemPreviewScenario(
            title = "Balance – Unreachable",
            item = single(balanceState = balanceUnreachable(), isBalanceHidden = false),
        ),
    )

    fun allGroupedScenarios(): List<GroupedUserAssetItemPreviewScenario> = listOf(
        GroupedUserAssetItemPreviewScenario(
            title = "Hidden (balanceState ignored)",
            item = grouped(
                balanceState = balanceLoaded(),
                isBalanceHidden = true,
            ),
        ),
        GroupedUserAssetItemPreviewScenario(
            title = "Balance – Loaded",
            item = grouped(balanceState = balanceLoaded(), isBalanceHidden = false),
        ),
        GroupedUserAssetItemPreviewScenario(
            title = "Balance – Flickering",
            item = grouped(balanceState = balanceFlickering(), isBalanceHidden = false),
        ),
        GroupedUserAssetItemPreviewScenario(
            title = "Balance – Stale",
            item = grouped(balanceState = balanceStale(), isBalanceHidden = false),
        ),
        GroupedUserAssetItemPreviewScenario(
            title = "Balance – Loading",
            item = grouped(balanceState = balanceLoading(), isBalanceHidden = false),
        ),
        GroupedUserAssetItemPreviewScenario(
            title = "Balance – Unreachable",
            item = grouped(balanceState = balanceUnreachable(), isBalanceHidden = false),
        ),
    )

    private fun single(balanceState: BalanceDisplayState, isBalanceHidden: Boolean): UserAssetItemUM.Single =
        UserAssetItemUM.Single(
            id = "single_preview",
            icon = sampleIcon,
            tokenName = "Ethereum",
            tokenSymbol = "ETH",
            fiatRate = "$98,765.43",
            priceChangeState = samplePriceChange,
            balanceState = balanceState,
            isBalanceHidden = isBalanceHidden,
            onClick = {},
            networkName = "Ethereum",
        )

    private fun grouped(balanceState: BalanceDisplayState, isBalanceHidden: Boolean): UserAssetItemUM.Grouped =
        UserAssetItemUM.Grouped(
            id = "grouped_preview",
            icon = sampleIcon,
            tokenName = "Ethereum",
            tokenSymbol = "ETH",
            tokensCount = 3,
            balanceState = balanceState,
            isBalanceHidden = isBalanceHidden,
            onClick = {},
        )
}

internal class SingleUserAssetItemPreviewParameterProvider :
    PreviewParameterProvider<SingleUserAssetItemPreviewScenario> {
    override val values: Sequence<SingleUserAssetItemPreviewScenario>
        get() = UserAssetItemPreviewFixtures.allSingleScenarios().asSequence()
}

internal class GroupedUserAssetItemPreviewParameterProvider :
    PreviewParameterProvider<GroupedUserAssetItemPreviewScenario> {
    override val values: Sequence<GroupedUserAssetItemPreviewScenario>
        get() = UserAssetItemPreviewFixtures.allGroupedScenarios().asSequence()
}

@Composable
private fun SingleUserAssetItemPreviewHost(
    scenario: SingleUserAssetItemPreviewScenario,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(TangemTheme.colors2.surface.level1)
            .padding(8.dp),
    ) {
        Box(
            modifier = Modifier.background(
                color = TangemTheme.colors2.surface.level3,
                shape = RoundedCornerShape(TangemTheme.dimens2.x5),
            ),
        ) {
            SingleUserAssetItem(item = scenario.item, shouldUsePriceBlock = true)
        }
    }
}

@Composable
private fun GroupedUserAssetItemPreviewHost(
    scenario: GroupedUserAssetItemPreviewScenario,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(TangemTheme.colors2.surface.level1)
            .padding(8.dp),
    ) {
        GroupedUserAssetItem(item = scenario.item)
    }
}

@Composable
@Preview(name = "Single – all balance states (parameter)", showBackground = true, widthDp = 360)
@Preview(
    name = "Single – all balance states (night)",
    showBackground = true,
    widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
private fun SingleUserAssetItemPreview_AllBalanceStates(
    @PreviewParameter(SingleUserAssetItemPreviewParameterProvider::class) scenario: SingleUserAssetItemPreviewScenario,
) {
    TangemThemePreviewRedesign {
        SingleUserAssetItemPreviewHost(scenario = scenario)
    }
}

@Composable
@Preview(name = "Grouped – all balance states (parameter)", showBackground = true, widthDp = 360)
@Preview(
    name = "Grouped – all balance states (night)",
    showBackground = true,
    widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
private fun GroupedUserAssetItemPreview_AllBalanceStates(
    @PreviewParameter(GroupedUserAssetItemPreviewParameterProvider::class)
    scenario: GroupedUserAssetItemPreviewScenario,
) {
    TangemThemePreviewRedesign {
        GroupedUserAssetItemPreviewHost(scenario = scenario)
    }
}