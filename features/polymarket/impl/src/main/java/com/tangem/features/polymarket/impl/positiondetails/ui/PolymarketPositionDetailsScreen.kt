package com.tangem.features.polymarket.impl.positiondetails.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.components.haze.ProvideHaze
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.button.TangemButtonGroup
import com.tangem.core.ui.ds2.footer.TangemFooter
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
import com.tangem.features.polymarket.impl.common.ui.PolymarketLoadingState
import com.tangem.features.polymarket.impl.common.ui.PolymarketReloadPrompt
import com.tangem.features.polymarket.impl.positiondetails.ui.preview.PolymarketPositionDetailsPreviewProvider
import com.tangem.features.polymarket.impl.positiondetails.ui.state.PolymarketPositionDetailsUM
import com.tangem.features.polymarket.impl.positiondetails.ui.state.PolymarketPositionDetailsUM.Content
import com.tangem.features.polymarket.impl.positiondetails.ui.state.PolymarketPositionDetailsUM.Pnl

@Composable
internal fun PolymarketPositionDetailsScreen(state: PolymarketPositionDetailsUM, modifier: Modifier = Modifier) {
    var footerHeight by remember { mutableIntStateOf(0) }
    ProvideHaze {
        TangemTopBarScaffold(
            modifier = modifier,
            topBar = { TangemTopNavigation(endButton = { TangemButton.Close(onClick = state.onCloseClick) }) },
            overlay = {
                if (state.content is Content.Data) {
                    PositionDetailsFooter(
                        onClosePositionClick = state.content.onClosePositionClick,
                        onBuyMoreClick = state.content.onBuyMoreClick,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .onSizeChanged { size -> footerHeight = size.height },
                    )
                }
            },
        ) { contentPadding ->
            when (val content = state.content) {
                is Content.Data -> PositionDetailsContent(content, contentPadding, footerHeight)
                else -> Box(
                    modifier = Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    when (content) {
                        Content.Loading -> PolymarketLoadingState()
                        Content.Unavailable -> Text(
                            text = stringResourceSafe(R.string.prediction_positions_unavailable),
                            style = TangemTheme.typography3.subheading.medium,
                            color = TangemTheme.colors3.text.secondary,
                            textAlign = TextAlign.Center,
                        )
                        is Content.Error -> PolymarketReloadPrompt(
                            text = resourceReference(R.string.common_something_went_wrong),
                            onReloadClick = content.onRetry,
                        )
                        is Content.Data -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun PositionDetailsContent(state: Content.Data, contentPadding: PaddingValues, footerHeight: Int) {
    var explanation by rememberSaveable { mutableStateOf<Pnl?>(null) }
    val bottomPadding = with(LocalDensity.current) { footerHeight.toDp() } + 16.dp
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(contentPadding).padding(horizontal = 16.dp).padding(bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PolymarketPositionSummary(state.summary, modifier = Modifier.padding(bottom = 20.dp))
        state.notice?.let { PolymarketPositionNotice(it) }
        state.event?.let { PositionEvent(it) }
        PolymarketPositionMetrics(state = state.metrics, onPnlClick = { explanation = it })
        PositionInformation(state.information)
    }
    PolymarketPositionPnlSheet(pnl = explanation, onDismiss = { explanation = null })
}

@Composable
private fun PositionDetailsFooter(
    onClosePositionClick: (() -> Unit)?,
    onBuyMoreClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    TangemFooter(modifier = modifier) {
        TangemButtonGroup(
            modifier = Modifier.fillMaxWidth(),
            orientation = TangemButtonGroup.Orientation.Horizontal,
        ) {
            TangemButton(
                variant = TangemButton.Variant.Secondary,
                size = TangemButton.Size.X12,
                text = resourceReference(R.string.common_close),
                isEnabled = onClosePositionClick != null,
                onClick = { onClosePositionClick?.invoke() },
            )
            TangemButton(
                variant = TangemButton.Variant.Primary,
                size = TangemButton.Size.X12,
                text = resourceReference(R.string.common_buy),
                isEnabled = onBuyMoreClick != null,
                onClick = { onBuyMoreClick?.invoke() },
            )
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
    @PreviewParameter(PolymarketPositionDetailsPreviewProvider::class) content: Content,
) {
    TangemThemePreviewRedesign(alwaysShowBottomSheets = false) {
        PolymarketPositionDetailsScreen(PolymarketPositionDetailsUM(content = content, onCloseClick = {}))
    }
}