package com.tangem.features.polymarket.impl.positionresult.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.statusscreen.TangemStatusIcon
import com.tangem.core.ui.ds2.statusscreen.TangemStatusScreen
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_logo_tangem_24
import com.tangem.features.polymarket.impl.placeprediction.ui.components.PredictionMarketBlock
import com.tangem.features.polymarket.impl.positionresult.ui.preview.PolymarketPositionResultPreviewProvider
import com.tangem.features.polymarket.impl.positionresult.ui.state.PolymarketPositionResultUM
import com.tangem.features.polymarket.impl.positionresult.ui.state.PolymarketPositionResultUM.Action

@Composable
internal fun PolymarketPositionResultScreen(
    state: PolymarketPositionResultUM,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TangemStatusScreen(
        modifier = modifier,
        variant = state.result.variant(),
        title = state.title,
        description = state.resolvedAt,
        asset = { ResultIcon(state.result) },
        details = {
            PredictionMarketBlock(state = state.market)
            ResultRows(state = state)
        },
        footer = { ResultFooter(note = state.footerNote, action = state.action, onActionClick = onActionClick) },
    )
}

@Composable
private fun ResultIcon(result: PolymarketPositionResultUM.Result) {
    TangemStatusIcon(
        kind = when (result) {
            PolymarketPositionResultUM.Result.Won -> TangemStatusIcon.Kind.Checkmark
            PolymarketPositionResultUM.Result.Lost -> TangemStatusIcon.Kind.HeartBroken
            PolymarketPositionResultUM.Result.Split -> TangemStatusIcon.Kind.Info
        },
        modifier = Modifier.size(40.dp),
    )
}

@Composable
private fun ResultRows(state: PolymarketPositionResultUM) {
    if (state.invested == null && state.profit == null) return

    Column(modifier = Modifier.fillMaxWidth()) {
        state.invested?.let { metric ->
            ResultMetric(metric = metric, divider = state.profit != null)
        }
        state.profit?.let { metric ->
            ResultMetric(metric = metric, valueColor = TangemTheme.colors3.text.accent.blue)
        }
    }
}

@Composable
private fun ResultMetric(
    metric: PolymarketPositionResultUM.Metric,
    divider: Boolean = false,
    valueColor: Color = TangemTheme.colors3.text.secondary,
) {
    TangemRow(
        modifier = Modifier.fillMaxWidth().height(44.dp),
        divider = divider,
        includeInnerPaddings = false,
        contentLead = TangemRowContentLead.End,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        titleSlot = {
            TangemRowText(
                modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                text = metric.title,
                role = TangemRowTextRole.Title,
            )
        },
        valueSlot = {
            Text(
                modifier = Modifier.padding(end = 16.dp, top = 12.dp, bottom = 12.dp),
                text = metric.value.resolveReference(),
                style = TangemTheme.typography3.body.medium,
                color = valueColor,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

@Composable
private fun ResultFooter(note: TextReference?, action: PolymarketPositionResultUM.Action, onActionClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (note != null) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = note.resolveReference(),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
                textAlign = TextAlign.Center,
            )
        }
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            isLoading = action is Action.Claim && action.isInProgress,
            contentDescription = if (action is Action.Claim && action.isInProgress) {
                stringResourceSafe(R.string.common_in_progress)
            } else {
                null
            },
            iconEnd = when (action) {
                is Action.Claim -> TangemIconUM.Icon(imageVector = Icons.ic_logo_tangem_24)
                Action.Dismiss -> null
            },
            text = when (action) {
                is Action.Claim -> action.label
                Action.Dismiss -> resourceReference(R.string.prediction_position_result_dismiss_action)
            },
            onClick = onActionClick,
        )
    }
}

private fun PolymarketPositionResultUM.Result.variant(): TangemStatusScreen.Variant = when (this) {
    PolymarketPositionResultUM.Result.Won -> TangemStatusScreen.Variant.Success
    PolymarketPositionResultUM.Result.Lost -> TangemStatusScreen.Variant.Error
    PolymarketPositionResultUM.Result.Split -> TangemStatusScreen.Variant.Info
}

@Preview(name = "Light", widthDp = 360, heightDp = 800)
@Preview(name = "Dark", widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PolymarketPositionResultScreenPreview(
    @PreviewParameter(PolymarketPositionResultPreviewProvider::class) state: PolymarketPositionResultUM,
) {
    TangemThemePreviewRedesign(alwaysShowBottomSheets = false) {
        PolymarketPositionResultScreen(state = state, onActionClick = {})
    }
}