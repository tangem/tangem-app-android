package com.tangem.features.polymarket.impl.main.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds.button.action.ActionButtons
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_down_24
import com.tangem.core.ui.res.generated.icons.ic_arrow_up_24
import com.tangem.core.ui.res.generated.icons.ic_chevron_double_vertical_16
import com.tangem.features.polymarket.impl.common.formatPolymarketMoneyStyled
import com.tangem.features.polymarket.impl.common.funding.PredictionAwaitingFundsBanner
import com.tangem.features.polymarket.impl.main.ui.state.PredictionBalanceUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketPositionsPreviewUM
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

/** Scrolling header of the Discovery feed: the account balance, its actions, and the events section title. */
@Composable
internal fun PolymarketMainHeader(
    balance: PredictionBalanceUM,
    isAwaitingFunds: Boolean,
    modifier: Modifier = Modifier,
    positions: PolymarketPositionsPreviewUM = PolymarketPositionsPreviewUM.Content(persistentListOf()),
    onActivePositionsClick: (() -> Unit)? = null,
    onFinishedPositionsClick: (() -> Unit)? = null,
) {
    val arePositionsVisible = positions.isVisible || onFinishedPositionsClick != null
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BalanceBlock(
            balance = balance,
            modifier = Modifier.padding(top = 56.dp),
        )

        ActionsRow(
            addFunds = balance.addFunds,
            withdraw = balance.withdraw,
            modifier = Modifier.padding(top = 32.dp),
        )

        if (isAwaitingFunds) {
            PredictionAwaitingFundsBanner(
                modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
            )
        }

        PolymarketPositionsPreview(
            state = positions,
            modifier = Modifier.padding(top = 48.dp),
            onActiveClick = onActivePositionsClick,
            onFinishedClick = onFinishedPositionsClick,
        )

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (arePositionsVisible) 32.dp else 72.dp, start = 16.dp, end = 16.dp),
            text = stringResourceSafe(R.string.prediction_main_explore_events),
            color = TangemTheme.colors3.text.primary,
            style = TangemTheme.typography3.heading.small,
        )
    }
}

@Composable
private fun BalanceBlock(balance: PredictionBalanceUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .clickable(enabled = balance.isSwitchEnabled, role = Role.Button, onClick = balance.onSwitchClick)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = balance.label.resolveReference(),
                color = TangemTheme.colors3.text.primary,
                style = TangemTheme.typography3.subheading.medium,
            )

            Icon(
                modifier = Modifier.size(16.dp),
                imageVector = Icons.ic_chevron_double_vertical_16,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.secondary,
            )
        }

        Text(
            text = balance.balance.resolveAnnotatedReference(),
            color = TangemTheme.colors3.text.primary,
            style = TangemTheme.typography3.display.medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            autoSize = TextAutoSize.StepBased(
                minFontSize = TangemTheme.typography3.heading.medium.fontSize,
                maxFontSize = TangemTheme.typography3.display.medium.fontSize,
            ),
        )
    }
}

@Composable
private fun ActionsRow(addFunds: TangemButtonUM, withdraw: TangemButtonUM, modifier: Modifier = Modifier) {
    // The buttons are material, but they sit inside the scaffold's own haze source, where haze has no area
    // left to sample and the material paints no fill. Their own state, sourced from the plate behind them,
    // gives it one.
    val backdropHazeState = rememberHazeState()

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .hazeSourceTangem(backdropHazeState)
                .background(TangemTheme.colors3.bg.primary),
        )

        CompositionLocalProvider(LocalHazeState provides backdropHazeState) {
            ActionButtons(buttons = persistentListOf(addFunds, withdraw))
        }
    }
}

@Preview(name = "Header Light", showBackground = true, widthDp = 360)
@Preview(
    name = "Header Dark",
    showBackground = true,
    widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PolymarketMainHeaderPreview(
    @PreviewParameter(PolymarketMainHeaderPreviewProvider::class) balance: BigDecimal,
) {
    TangemThemePreviewRedesign {
        val state = previewPredictionBalance(balance)

        PolymarketMainHeader(
            balance = state,
            isAwaitingFunds = false,
            modifier = Modifier.background(TangemTheme.colors3.bg.primary),
        )
    }
}

private class PolymarketMainHeaderPreviewProvider : CollectionPreviewParameterProvider<BigDecimal>(
    collection = listOf(BigDecimal.ZERO, BigDecimal("1234.56")),
)

@Preview(name = "Header Awaiting Funds Light", showBackground = true, widthDp = 360)
@Preview(
    name = "Header Awaiting Funds Dark",
    showBackground = true,
    widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PolymarketMainHeaderAwaitingFundsPreview() {
    TangemThemePreviewRedesign {
        val state = previewPredictionBalance(BigDecimal("1234.56"))

        PolymarketMainHeader(
            balance = state,
            isAwaitingFunds = true,
            modifier = Modifier.background(TangemTheme.colors3.bg.primary),
        )
    }
}

internal fun previewPredictionBalance(balance: BigDecimal) = PredictionBalanceUM(
    balance = balance.formatPolymarketMoneyStyled(
        fractionStyle = { TangemTheme.typography3.heading.medium.toSpanStyle() },
    ),
    label = resourceReference(R.string.prediction_main_available_balance),
    isSwitchEnabled = true,
    onSwitchClick = {},
    addFunds = previewButton(
        text = resourceReference(R.string.common_add_funds),
        icon = Icons.ic_arrow_down_24,
        isEnabled = true,
    ),
    withdraw = previewButton(
        text = resourceReference(R.string.prediction_main_withdraw),
        icon = Icons.ic_arrow_up_24,
        isEnabled = balance.signum() > 0,
    ),
)

private fun previewButton(text: TextReference, icon: ImageVector, isEnabled: Boolean) = TangemButtonUM(
    text = text,
    tangemIconUM = TangemIconUM.Icon(
        imageVector = icon,
        tintReference = { TangemTheme.colors3.icon.primary },
    ),
    type = TangemButtonType.Secondary,
    isEnabled = isEnabled,
    onClick = {},
)