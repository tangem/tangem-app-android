package com.tangem.features.polymarket.impl.history.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TopNavigation
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.rememberLastNonNull
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.polymarket.impl.common.ui.PolymarketCardIcon
import com.tangem.features.polymarket.impl.history.ui.state.PolymarketActivityDetailsUM

@Composable
internal fun PolymarketActivityDetailsSheet(state: PolymarketActivityDetailsUM?, onDismiss: () -> Unit) {
    val displayed = rememberLastNonNull(state) ?: return
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    var measuredSize by remember(displayed, density, configuration) { mutableStateOf<ActivityDetailsSize?>(null) }
    val size = measuredSize
    if (size == null) {
        MeasureActivityDetails(
            header = { ActivityDetailsHeader(displayed, onDismiss) },
            body = { ActivityDetailsBody(displayed) },
            onMeasure = { measuredSize = it },
        )
        return
    }
    TangemModal<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = state != null,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        showDragIndicator = true,
        title = {
            ActivityDetailsHeader(displayed, onDismiss, Modifier.height(with(density) { size.headerHeight.toDp() }))
        },
    ) {
        ActivityDetailsBody(displayed, Modifier.height(with(density) { size.bodyHeight.toDp() }))
    }
}

@Composable
private fun ActivityDetailsBody(displayed: PolymarketActivityDetailsUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        ActivityDetailsSummary(displayed, Modifier.padding(horizontal = 16.dp).padding(top = 32.dp, bottom = 48.dp))
        displayed.rows.forEachIndexed { index, row ->
            TangemRow(
                divider = index < displayed.rows.lastIndex,
                contentLead = TangemRowContentLead.Start,
                titleSlot = {
                    TangemRowText(row.title, TangemRowTextRole.Title, maxLines = Int.MAX_VALUE)
                },
                valueSlot = {
                    Text(
                        text = row.value.resolveReference(),
                        style = TangemTheme.typography3.body.medium,
                        color = TangemTheme.colors3.text.secondary,
                        textAlign = TextAlign.End,
                    )
                },
            )
        }
        displayed.onGoToPrediction?.let { onClick ->
            TangemButton(
                text = resourceReference(R.string.prediction_history_go_to_predict),
                variant = TangemButton.Variant.Material,
                size = TangemButton.Size.X11,
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun ActivityDetailsHeader(
    state: PolymarketActivityDetailsUM,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TangemModal.TopNavigation(
        modifier = modifier,
        title = state.title,
        subtitle = state.date,
        onClose = onDismiss,
    )
}

@Composable
private fun ActivityDetailsSummary(state: PolymarketActivityDetailsUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val iconModifier = Modifier.padding(bottom = 16.dp).size(72.dp)
        PolymarketCardIcon(iconUrl = state.iconUrl, modifier = iconModifier)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            state.headline?.let { ActivityHeadline(it) }
            state.amount?.let { ActivityHeadline(it) }
        }
        state.marketTitle?.let { title ->
            Text(
                text = title.resolveReference(),
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.secondary,
                textAlign = TextAlign.Center,
            )
        }
        state.outcome?.let { outcome ->
            ActivityOutcomeBadge(outcome, state.outcomeStatus, state.outcomePrice)
        }
    }
}

@Composable
private fun ActivityOutcomeBadge(outcome: TextReference, status: TangemBadge.Status, price: TextReference?) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = resourceReference(R.string.prediction_place_you_vote).resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
        )
        TangemBadge(text = outcome, status = status, size = TangemBadge.Size.X4)
        price?.let { TangemBadge(text = it, size = TangemBadge.Size.X4) }
    }
}

@Composable
private fun ActivityHeadline(text: TextReference) {
    Text(
        text = text.resolveReference(),
        style = TangemTheme.typography3.heading.medium,
        color = TangemTheme.colors3.text.primary,
        textAlign = TextAlign.Center,
    )
}