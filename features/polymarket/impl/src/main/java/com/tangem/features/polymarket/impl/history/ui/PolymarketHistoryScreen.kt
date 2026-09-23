package com.tangem.features.polymarket.impl.history.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.components.haze.ProvideHaze
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.ds2.tabnavigation.TangemTabNavigation
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_globe_20
import com.tangem.core.ui.res.generated.icons.ic_info_20
import com.tangem.features.polymarket.impl.common.ui.PolymarketLoadingState
import com.tangem.features.polymarket.impl.common.ui.PolymarketReloadPrompt
import com.tangem.features.polymarket.impl.common.ui.PolymarketSearchBarClearance
import com.tangem.features.polymarket.impl.history.ui.state.PolymarketHistoryUM
import com.tangem.features.polymarket.impl.history.ui.state.PolymarketHistoryUM.Content

@Composable
internal fun PolymarketHistoryScreen(state: PolymarketHistoryUM, modifier: Modifier = Modifier) {
    val layoutDirection = LocalLayoutDirection.current
    ProvideHaze {
        TangemTopBarScaffold(
            modifier = modifier.fillMaxSize().imePadding(),
            topBar = { HistoryTopBar(state) },
            overlay = { contentPadding ->
                val content = state.content
                if (content is Content.Data) {
                    TangemSearch(
                        state = content.search,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(
                                start = contentPadding.calculateStartPadding(layoutDirection) + 16.dp,
                                end = contentPadding.calculateEndPadding(layoutDirection) + 16.dp,
                                bottom = contentPadding.calculateBottomPadding() + 8.dp,
                            ),
                    )
                }
            },
        ) { contentPadding ->
            when (val content = state.content) {
                is Content.Data -> HistoryList(content, contentPadding)
                else -> Box(
                    modifier = Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    when (content) {
                        Content.Loading -> PolymarketLoadingState()
                        is Content.Empty -> EmptyHistory(content.onExploreEventsClick)
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
private fun HistoryTopBar(state: PolymarketHistoryUM) {
    Column(modifier = Modifier.background(TangemTheme.colors3.bg.primary)) {
        TangemTopNavigation(
            title = resourceReference(R.string.prediction_history_title),
            contentAlign = TangemTopNavigation.ContentAlign.Center,
            onBack = state.onBackClick,
            blurBackground = false,
            fadeEnabled = false,
            endButton = {
                val content = state.content
                if (content is Content.Data) PolymarketHistoryMenu(onDownloadClick = content.onDownloadCsvClick)
            },
        )
        val content = state.content
        if (content is Content.Data) {
            TangemTabNavigation(
                tabs = content.tabs,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun HistoryList(content: Content.Data, contentPadding: PaddingValues) {
    val layoutDirection = LocalLayoutDirection.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + PolymarketSearchBarClearance,
            start = contentPadding.calculateStartPadding(layoutDirection),
            end = contentPadding.calculateEndPadding(layoutDirection),
        ),
    ) {
        content.groups.forEach { group ->
            item(key = "date:${group.id}", contentType = "date") {
                PolymarketActivityDateHeader(title = group.title)
            }
            items(group.activities, key = { "activity:${it.id}" }, contentType = { "activity" }) { activity ->
                PolymarketActivityRow(state = activity)
            }
        }
    }
}

@Composable
private fun EmptyHistory(onExploreEventsClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(TangemTheme.colors3.bg.opaque.secondary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.ic_info_20,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.secondary,
            )
        }
        Text(
            text = stringResourceSafe(R.string.prediction_history_empty),
            color = TangemTheme.colors3.text.primary,
            style = TangemTheme.typography3.subheading.medium,
            textAlign = TextAlign.Center,
        )
        TangemButton(
            text = resourceReference(R.string.prediction_main_explore_events),
            iconEnd = TangemIconUM.Icon(Icons.ic_globe_20),
            variant = TangemButton.Variant.Secondary,
            size = TangemButton.Size.X9,
            onClick = onExploreEventsClick,
        )
    }
}