package com.tangem.features.polymarket.impl.soccer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TopNavigation
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItem
import com.tangem.core.ui.ds2.tabnavigation.TangemTabNavigation
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.polymarket.impl.common.feed.ui.PolymarketSportsHeader
import com.tangem.features.polymarket.impl.details.model.chart.PolymarketEventChartSession
import com.tangem.features.polymarket.impl.details.ui.PolymarketEventDetailsChart
import com.tangem.features.polymarket.impl.details.ui.PolymarketEventDetailsScaffold
import com.tangem.features.polymarket.impl.details.ui.EVENT_DETAILS_HEADER_KEY
import com.tangem.features.polymarket.impl.details.ui.EventDetailsErrorState
import com.tangem.features.polymarket.impl.details.ui.EventDetailsLoadingState
import com.tangem.features.polymarket.impl.details.ui.EventMetaRow
import com.tangem.features.polymarket.impl.details.ui.summarySection
import com.tangem.features.polymarket.impl.soccer.ui.state.PolymarketSoccerCardUM
import com.tangem.features.polymarket.impl.soccer.ui.state.PolymarketSoccerUM

@Suppress("LongParameterList", "LongMethod")
@Composable
internal fun PolymarketSoccerScreen(
    state: PolymarketSoccerUM,
    chart: PolymarketEventChartSession.Snapshot?,
    onChartPeriodChange: (String) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOutcomeClick: (PolymarketSoccerCardUM.Target) -> Unit,
    onRules: (PolymarketSoccerCardUM.Rules) -> Unit,
    onPositionChange: (String, String) -> Unit,
    onReadMore: () -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val content = state as? PolymarketSoccerUM.Content
    PolymarketEventDetailsScaffold(
        modifier = modifier,
        listState = listState,
        title = content?.title,
        categoryLabel = content?.league,
        onBackClick = onBack,
    ) { padding ->
        when (state) {
            PolymarketSoccerUM.Loading -> EventDetailsLoadingState(Modifier.fillMaxSize().padding(padding))
            PolymarketSoccerUM.Error -> EventDetailsErrorState(onRetry, Modifier.fillMaxSize().padding(padding))
            is PolymarketSoccerUM.Content -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = EVENT_DETAILS_HEADER_KEY) { SoccerHeader(state) }
                item(key = "score") {
                    PolymarketSportsHeader(
                        state = state.sports,
                        modifier = Modifier.padding(16.dp),
                        showTeamNames = false,
                    )
                }
                if (chart != null) {
                    item(key = "chart") { PolymarketEventDetailsChart(chart, onChartPeriodChange) }
                }
                item(key = "tabs") {
                    TangemTabNavigation(
                        tabs = state.tabs,
                        variant = TangemTabItem.Variant.Material,
                        contentPadding = PaddingValues(horizontal = 16.dp),
                    )
                }
                items(items = state.cards, key = { it.id }) { card ->
                    PolymarketSoccerCard(
                        state = card,
                        onOutcomeClick = onOutcomeClick,
                        onRulesClick = onRules,
                        onPositionChange = { onPositionChange(card.id, it) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                summarySection(
                    description = state.description,
                    isDescriptionExpanded = state.isDescriptionExpanded,
                    onReadMoreClick = onReadMore,
                    resolutionDate = state.resolutionDate,
                    marketOpenedDate = state.marketOpenedDate,
                    resolutionSource = state.resolutionSource,
                    onResolutionSourceClick = onOpenUrl,
                )
            }
        }
    }
}

@Composable
private fun SoccerHeader(state: PolymarketSoccerUM.Content) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), Arrangement.spacedBy(8.dp)) {
        Text(
            text = state.title.resolveReference(),
            color = TangemTheme.colors3.text.primary,
            style = TangemTheme.typography3.heading.medium,
        )
        EventMetaRow(categoryLabel = state.league, totalVolume = state.volume, change24h = state.change24h)
        if (!state.sports.isLive && state.sports.homeScore == null && state.kickoff != null) {
            Text(
                text = state.kickoff.resolveReference(),
                color = TangemTheme.colors3.text.secondary,
                style = TangemTheme.typography3.caption.medium,
            )
        }
    }
}

@Composable
internal fun SoccerRulesSheet(state: PolymarketSoccerCardUM.Rules, onDismiss: () -> Unit) {
    TangemModal<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        showDragIndicator = true,
        title = { TangemModal.TopNavigation(title = state.title, onClose = onDismiss) },
    ) {
        Text(
            modifier = Modifier.padding(20.dp),
            text = state.description,
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.secondary,
        )
    }
}