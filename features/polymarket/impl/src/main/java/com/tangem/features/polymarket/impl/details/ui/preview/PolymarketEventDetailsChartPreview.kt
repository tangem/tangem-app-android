package com.tangem.features.polymarket.impl.details.ui.preview

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.features.polymarket.impl.details.model.transformer.PolymarketEventDetailsContentTransformer
import com.tangem.features.polymarket.impl.details.ui.PolymarketEventDetailsScreen
import com.tangem.features.polymarket.impl.details.ui.state.PolymarketEventDetailsUM

/** Runs the real details layout and content mapper with caller-owned fixtures, without a wallet or network model. */
@VisibleForTesting
@Composable
fun PolymarketEventDetailsChartPreview(
    event: PolymarketEvent,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    chart: @Composable () -> Unit,
) {
    var isExpanded by remember(event.id) { mutableStateOf(false) }
    var isDescriptionExpanded by remember(event.id) { mutableStateOf(false) }
    val state = PolymarketEventDetailsContentTransformer(
        event = event,
        isClosedMarketsExpanded = isExpanded,
        isDescriptionExpanded = isDescriptionExpanded,
        onShareClick = {},
        onOutcomeClick = { _, _ -> },
        onClosedMarketsClick = { isExpanded = !isExpanded },
        onReadMoreClick = { isDescriptionExpanded = true },
    ).transform(PolymarketEventDetailsUM.Loading)
    PolymarketEventDetailsScreen(state = state, onBackClick = onBackClick, modifier = modifier, chart = chart)
}