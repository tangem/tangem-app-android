package com.tangem.features.polymarket.impl.positions.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.components.haze.ProvideHaze
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.ds2.tabnavigation.TangemTabNavigation
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.domain.polymarket.model.PolymarketPositionsFilter
import com.tangem.features.polymarket.impl.common.ui.PolymarketLoadMoreEffect
import com.tangem.features.polymarket.impl.common.ui.PolymarketLoadingState
import com.tangem.features.polymarket.impl.common.ui.PolymarketNextPageLoader
import com.tangem.features.polymarket.impl.common.ui.PolymarketReloadPrompt
import com.tangem.features.polymarket.impl.positiondetails.ui.PolymarketFinishedPositionSheet
import com.tangem.features.polymarket.impl.positions.ui.preview.PolymarketPositionsPreviewCase
import com.tangem.features.polymarket.impl.positions.ui.preview.PolymarketPositionsPreviewProvider
import com.tangem.features.polymarket.impl.positions.ui.state.PolymarketPositionsUM
import com.tangem.features.polymarket.impl.positions.ui.state.PolymarketPositionsUM.Content
import com.tangem.features.polymarket.impl.positions.ui.state.PolymarketPositionsUM.NextPage
import com.tangem.features.polymarket.impl.positions.ui.state.PolymarketPositionsUM.Tab
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun PolymarketPositionsScreen(state: PolymarketPositionsUM, modifier: Modifier = Modifier) {
    ProvideHaze {
        TangemTopBarScaffold(
            modifier = modifier,
            topBar = { PositionsTopBar(state) },
        ) { contentPadding ->
            key(state.selectedTab, state.selectedFilter) {
                val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
                when (val content = state.content) {
                    is Content.Data -> PositionsList(content, listState, contentPadding)
                    else -> Box(
                        modifier = Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        when (content) {
                            Content.Loading -> PolymarketLoadingState()
                            Content.Empty -> EmptyPositions(state.selectedTab, state.selectedFilter)
                            is Content.Error -> PositionsRetry(content.onRetry)
                            is Content.Data -> Unit
                        }
                    }
                }
            }
        }
        PolymarketFinishedPositionSheet(state = state.finishedPosition, onDismiss = state.onFinishedPositionDismiss)
    }
}

@Composable
private fun PositionsTopBar(state: PolymarketPositionsUM) {
    Column(modifier = Modifier.background(TangemTheme.colors3.bg.primary)) {
        TangemTopNavigation(
            title = resourceReference(R.string.prediction_positions_title),
            contentAlign = TangemTopNavigation.ContentAlign.Center,
            onBack = state.onBackClick,
            blurBackground = false,
            fadeEnabled = false,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PositionsTabs(state, Modifier.weight(1f))
            if (state.selectedTab == Tab.Active) {
                PolymarketPositionsFilterDropdown(selected = state.selectedFilter, onSelect = state.onFilterSelect)
            }
        }
    }
}

@Composable
private fun PositionsTabs(state: PolymarketPositionsUM, modifier: Modifier = Modifier) {
    TangemTabNavigation(
        modifier = modifier,
        contentPadding = PaddingValues(0.dp),
        tabs = persistentListOf(
            TangemTabItemUM.Content(
                id = Tab.Active.name,
                label = resourceReference(R.string.prediction_positions_active),
                isSelected = state.selectedTab == Tab.Active,
                onClick = { state.onTabSelect(Tab.Active) },
            ),
            TangemTabItemUM.Content(
                id = Tab.Finished.name,
                label = resourceReference(R.string.prediction_positions_finished),
                isSelected = state.selectedTab == Tab.Finished,
                onClick = { state.onTabSelect(Tab.Finished) },
            ),
        ),
    )
}

@Composable
private fun PositionsList(content: Content.Data, listState: LazyListState, contentPadding: PaddingValues) {
    val layoutDirection = LocalLayoutDirection.current
    val nextPage = content.nextPage
    if (nextPage is NextPage.Ready) {
        // Recheck after a short page even when the end stayed visible throughout loading.
        key(content.positions.size) {
            PolymarketLoadMoreEffect(listState = listState, onLoadMore = nextPage.onLoadMore)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
            start = contentPadding.calculateStartPadding(layoutDirection) + 16.dp,
            end = contentPadding.calculateEndPadding(layoutDirection) + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = content.positions, key = { "position:${it.id}" }, contentType = { "position" }) { position ->
            PolymarketPositionCard(state = position, modifier = Modifier.fillMaxWidth())
        }
        when (nextPage) {
            NextPage.Loading -> item(key = "next_page", contentType = "loader") {
                PolymarketNextPageLoader()
            }
            is NextPage.Error -> item(key = "next_page", contentType = "retry") {
                PositionsRetry(nextPage.onRetry)
            }
            NextPage.End, is NextPage.Ready -> Unit
        }
    }
}

@Composable
private fun EmptyPositions(tab: Tab, filter: PolymarketPositionsFilter) {
    Text(
        text = stringResourceSafe(
            when {
                filter != PolymarketPositionsFilter.ALL -> R.string.common_no_results
                tab == Tab.Active -> R.string.prediction_positions_empty_active
                else -> R.string.prediction_positions_empty_finished
            },
        ),
        color = TangemTheme.colors3.text.secondary,
        style = TangemTheme.typography3.subheading.medium,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun PositionsRetry(onRetry: () -> Unit) {
    PolymarketReloadPrompt(
        text = resourceReference(R.string.prediction_positions_load_error),
        onReloadClick = onRetry,
    )
}

@Preview(name = "Light", group = "Positions screen", widthDp = 360, heightDp = 800)
@Preview(
    name = "Dark",
    group = "Positions screen",
    widthDp = 360,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Preview(name = "Large text", group = "Positions screen", widthDp = 320, heightDp = 800, fontScale = 1.5f)
@Composable
private fun PolymarketPositionsScreenPreview(
    @PreviewParameter(PolymarketPositionsPreviewProvider::class) case: PolymarketPositionsPreviewCase,
) {
    var selectedTab by remember(case) { mutableStateOf(case.selectedTab) }
    var selectedFilter by remember(case) { mutableStateOf(case.selectedFilter) }
    TangemThemePreviewRedesign {
        PolymarketPositionsScreen(
            state = PolymarketPositionsUM(
                selectedTab = selectedTab,
                content = if (selectedTab == Tab.Active) case.active else case.finished,
                onTabSelect = { tab ->
                    selectedTab = tab
                    selectedFilter = PolymarketPositionsFilter.ALL
                },
                onBackClick = {},
                selectedFilter = selectedFilter,
                onFilterSelect = { selectedFilter = it },
            ),
        )
    }
}