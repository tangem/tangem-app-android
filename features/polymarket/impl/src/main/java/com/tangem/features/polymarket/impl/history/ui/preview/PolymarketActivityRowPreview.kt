package com.tangem.features.polymarket.impl.history.ui.preview

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.R
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.polymarket.impl.history.ui.PolymarketActivityDateHeader
import com.tangem.features.polymarket.impl.history.ui.PolymarketActivityRow
import com.tangem.features.polymarket.impl.history.ui.state.PolymarketActivityRowUM
import com.tangem.features.polymarket.impl.history.ui.state.PolymarketActivityRowUM.Type
import com.tangem.features.polymarket.impl.history.ui.state.PolymarketActivitySubtitleUM.Account
import com.tangem.features.polymarket.impl.history.ui.state.PolymarketActivitySubtitleUM.Direction
import com.tangem.features.polymarket.impl.history.ui.state.PolymarketActivitySubtitleUM.Funding
import com.tangem.features.polymarket.impl.history.ui.state.PolymarketActivitySubtitleUM.Plain

@Preview(name = "Light", group = "Activity states", widthDp = 360)
@Preview(name = "Dark", group = "Activity states", widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large text", group = "Activity states", widthDp = 320, fontScale = 1.5f)
@Composable
private fun ActivityRowPreview(@PreviewParameter(ActivityRowPreviewProvider::class) case: ActivityRowPreviewCase) {
    TangemThemePreviewRedesign {
        Column(modifier = Modifier.fillMaxWidth().background(TangemTheme.colors3.bg.primary)) {
            PreviewRow(case = case)
        }
    }
}

@Preview(name = "Light", group = "Date groups", widthDp = 390)
@Preview(name = "Dark", group = "Date groups", widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large text", group = "Date groups", widthDp = 320, fontScale = 1.5f)
@Composable
private fun ActivityDateGroupsPreview() {
    TangemThemePreviewRedesign {
        Column(modifier = Modifier.fillMaxWidth().background(TangemTheme.colors3.bg.primary)) {
            activityCases().forEachIndexed { index, case ->
                if (index == 0 || index == 3) {
                    PolymarketActivityDateHeader(
                        title = if (index == 0) {
                            resourceReference(R.string.common_today)
                        } else {
                            stringReference("June 12, 2026")
                        },
                    )
                }
                PreviewRow(case = case)
            }
        }
    }
}

@Composable
private fun PreviewRow(case: ActivityRowPreviewCase) {
    val iconUrl = if (case.shouldShowImage) {
        "android.resource://${LocalContext.current.packageName}/${R.drawable.img_eth_22}"
    } else {
        null
    }
    PolymarketActivityRow(state = case.state.copy(iconUrl = iconUrl))
}

internal data class ActivityRowPreviewCase(
    val name: String,
    val state: PolymarketActivityRowUM,
    val shouldShowImage: Boolean = true,
)

internal class ActivityRowPreviewProvider : CollectionPreviewParameterProvider<ActivityRowPreviewCase>(
    activityCases() + layoutCases(),
) {
    override fun getDisplayName(index: Int): String = values.elementAt(index).name
}

private fun activityCases(): List<ActivityRowPreviewCase> = listOf(
    activityCase(Type.WITHDRAWN, "Withdrawn", "- $5,000.00").let { case ->
        case.copy(
            state = case.state.copy(
                subtitle = fundingSubtitle(Direction.TO, "Family"),
            ),
        )
    },
    activityCase(Type.LOST, "Lost", "-"),
    activityCase(Type.REDEEMED, "Redeemed", "+ $350.31"),
    activityCase(Type.BOUGHT, "Bought", "- $200.01"),
    activityCase(Type.SOLD, "Sold", "+ $200.01"),
    activityCase(Type.FUNDS_ADDED, "Funds Added", "+ $5,000.31").let { case ->
        case.copy(state = case.state.copy(subtitle = fundingSubtitle(Direction.FROM, "Portfolio")))
    },
)

private fun layoutCases(): List<ActivityRowPreviewCase> {
    val bought = activityRow(Type.BOUGHT, "- $200.01")
    return listOf(
        ActivityRowPreviewCase(
            name = "Long event title",
            state = bought.copy(
                subtitle = Plain(
                    stringReference("Will Ethereum reach a new all-time high before the end of September 2026?"),
                ),
            ),
        ),
        ActivityRowPreviewCase(
            name = "Long operation title and large amount",
            state = bought.copy(
                title = stringReference("A long translated operation title"),
                amount = stringReference("- $12,345,678.90"),
            ),
        ),
        ActivityRowPreviewCase(
            name = "Missing event image",
            state = bought,
            shouldShowImage = false,
        ),
        ActivityRowPreviewCase(
            name = "Long account name",
            state = activityRow(Type.FUNDS_ADDED, "+ $5,000.31").copy(
                subtitle = fundingSubtitle(Direction.FROM, "Family long-term investment portfolio"),
            ),
        ),
        ActivityRowPreviewCase(
            name = "Funding without an account",
            state = activityRow(Type.WITHDRAWN, "- $10.00").copy(
                subtitle = fundingSubtitle(Direction.TO, "Family").copy(account = null),
            ),
        ),
        ActivityRowPreviewCase(
            name = "Funding without a token icon",
            state = activityRow(Type.FUNDS_ADDED, "+ $10.00").copy(
                subtitle = fundingSubtitle(Direction.FROM, "Family").copy(icon = null),
            ),
        ),
        activityCase(Type.REDEEMED, "Redeemed", "+ $0.00"),
    )
}

private fun activityCase(type: Type, title: String, amount: String) = ActivityRowPreviewCase(
    name = title,
    state = activityRow(type, amount),
)

private fun activityRow(type: Type, amount: String) = PolymarketActivityRowUM(
    id = type.name,
    type = type,
    title = resourceReference(
        when (type) {
            Type.BOUGHT -> R.string.prediction_history_bought
            Type.SOLD -> R.string.prediction_history_sold
            Type.FUNDS_ADDED -> R.string.prediction_history_funds_added
            Type.WITHDRAWN -> R.string.transaction_history_withdrawn
            Type.REDEEMED -> R.string.prediction_history_redeemed
            Type.LOST -> R.string.prediction_positions_lost
        },
    ),
    subtitle = Plain(stringReference("Will Ethereum reach $5,000 before September 30?")),
    amount = stringReference(amount),
    onClick = {},
)

@Suppress("MagicNumber")
private fun fundingSubtitle(direction: Direction, accountName: String) = Funding(
    direction = direction,
    symbol = "ETH",
    icon = CurrencyIconState.CoinIcon(
        url = null,
        fallbackResId = R.drawable.img_eth_22,
        isGrayscale = false,
        shouldShowCustomBadge = false,
    ),
    account = Account(
        name = stringReference(accountName),
        iconResId = R.drawable.ic_star_24,
        iconBackgroundColor = Color(0xFF0088FF),
    ),
)