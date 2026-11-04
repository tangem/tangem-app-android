package com.tangem.features.polymarket.impl.category.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.containers.pullToRefresh.TangemPullToRefreshSlidingContainer
import com.tangem.core.ui.components.haze.ProvideHaze
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.shimmers.ProvideTangemShimmer
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItem
import com.tangem.core.ui.ds2.tabnavigation.TangemTabNavigation
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.features.polymarket.impl.category.ui.state.PolymarketCategoryUM
import com.tangem.features.polymarket.impl.common.ui.PolymarketLoadMoreEffect
import com.tangem.features.polymarket.impl.main.ui.PolymarketScrollIdleEffect
import com.tangem.features.polymarket.impl.main.ui.PolymarketVisibleEventsEffect
import com.tangem.features.polymarket.impl.main.ui.polymarketEventsSection
import com.tangem.features.polymarket.impl.main.ui.toTangemTabItemUM
import kotlinx.collections.immutable.toImmutableList

@Suppress("LongParameterList")
@Composable
internal fun PolymarketCategoryScreen(
    state: PolymarketCategoryUM,
    onBack: () -> Unit,
    onLoadMore: () -> Unit,
    onVisibleEventsChange: (Set<String>) -> Unit,
    onScrollIdle: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    ProvideHaze {
        ProvideTangemShimmer {
            TangemTopBarScaffold(
                modifier = modifier,
                topBar = {
                    Column {
                        TangemTopNavigation(
                            title = state.title,
                            onBack = onBack,
                            contentAlign = TangemTopNavigation.ContentAlign.Center,
                            fadeEnabled = false,
                        )
                        CategoryTabs(state)
                    }
                },
                overlay = { padding ->
                    TangemFade(
                        position = TangemFade.Position.Top,
                        variant = TangemFade.Variant.Hard,
                        blur = true,
                        modifier = Modifier.height(padding.calculateTopPadding() + 12.dp),
                    )
                },
            ) { padding ->
                TangemPullToRefreshSlidingContainer(
                    config = state.pullToRefreshConfig,
                    indicatorOffset = padding.calculateTopPadding(),
                ) {
                    key(state.selection) {
                        val listState = rememberLazyListState()
                        PolymarketLoadMoreEffect(listState = listState, onLoadMore = onLoadMore)
                        PolymarketVisibleEventsEffect(
                            listState = listState,
                            feedKey = state.selection,
                            onVisibleEventsChange = onVisibleEventsChange,
                        )
                        PolymarketScrollIdleEffect(listState, state.selection, onScrollIdle)
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState,
                            contentPadding = PaddingValues(
                                top = padding.calculateTopPadding() + 12.dp,
                                bottom = padding.calculateBottomPadding() + 16.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            polymarketEventsSection(content = state.content, skeletonCount = 6)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTabs(state: PolymarketCategoryUM) {
    if (state.subcategories.isNotEmpty()) {
        TangemTabNavigation(
            tabs = state.subcategories.map { it.toTangemTabItemUM() }.toImmutableList(),
            variant = TangemTabItem.Variant.Material,
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.padding(top = 8.dp),
        )
    }
    if (state.filters.isNotEmpty()) {
        TangemTabNavigation(
            tabs = state.filters,
            variant = TangemTabItem.Variant.Material,
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.padding(vertical = 12.dp),
        )
    }
}