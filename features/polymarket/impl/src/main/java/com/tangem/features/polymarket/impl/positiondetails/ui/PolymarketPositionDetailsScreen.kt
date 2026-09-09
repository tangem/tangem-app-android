package com.tangem.features.polymarket.impl.positiondetails.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.components.haze.ProvideHaze
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.polymarket.impl.common.ui.PolymarketEventCardFrame
import com.tangem.features.polymarket.impl.positiondetails.ui.preview.PolymarketPositionDetailsPreviewProvider
import com.tangem.features.polymarket.impl.positiondetails.ui.state.PolymarketPositionDetailsUM

@Composable
internal fun PolymarketPositionDetailsScreen(state: PolymarketPositionDetailsUM, modifier: Modifier = Modifier) {
    ProvideHaze {
        TangemTopBarScaffold(
            modifier = modifier,
            topBar = { TangemTopNavigation(endButton = { TangemButton.Close(onClick = state.onCloseClick) }) },
        ) { contentPadding ->
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(contentPadding).padding(horizontal = 16.dp).padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PolymarketPositionSummary(state.summary, modifier = Modifier.padding(bottom = 20.dp))
                state.event?.let { PositionEvent(it) }
                PolymarketPositionMetrics(state.metrics)
                PositionInformation(state.information)
            }
        }
    }
}

@Composable
private fun PositionEvent(state: PolymarketPositionDetailsUM.Event) {
    PolymarketEventCardFrame(
        title = state.title,
        iconUrl = state.iconUrl,
        onClick = state.onClick,
        meta = state.volume?.let { volume ->
            {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResourceSafe(R.string.prediction_event_total_volume),
                        style = TangemTheme.typography3.caption.medium,
                        color = TangemTheme.colors3.text.secondary,
                    )
                    Text(
                        text = volume.resolveReference(),
                        style = TangemTheme.typography3.caption.medium,
                        color = TangemTheme.colors3.text.primary,
                    )
                }
            }
        },
    )
}

@Composable
private fun PositionInformation(state: PolymarketPositionDetailsUM.Information) {
    Column {
        PositionInfoRow(resourceReference(R.string.prediction_positions_invested), state.invested, divider = true)
        PositionInfoRow(
            title = resourceReference(R.string.prediction_positions_status),
            value = state.status,
            divider = state.endDate != null,
        )
        state.endDate?.let {
            PositionInfoRow(resourceReference(R.string.prediction_details_resolution_date), it, divider = false)
        }
    }
}

@Composable
private fun PositionInfoRow(title: TextReference, value: TextReference, divider: Boolean) {
    TangemRow(
        divider = divider,
        titleSlot = { TangemRowText(text = title, role = TangemRowTextRole.Title, maxLines = Int.MAX_VALUE) },
        valueSlot = { TangemRowText(text = value, role = TangemRowTextRole.Value, maxLines = Int.MAX_VALUE) },
    )
}

@Preview(name = "Light", widthDp = 360, heightDp = 800)
@Preview(name = "Dark", widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large text", widthDp = 320, heightDp = 800, fontScale = 1.5f)
@Composable
private fun PolymarketPositionDetailsScreenPreview(
    @PreviewParameter(PolymarketPositionDetailsPreviewProvider::class) state: PolymarketPositionDetailsUM,
) {
    TangemThemePreviewRedesign { PolymarketPositionDetailsScreen(state) }
}