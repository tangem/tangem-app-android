package com.tangem.features.polymarket.impl.common.feed.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.polymarket.impl.common.ui.PolymarketEmptyListPrompt
import com.tangem.features.polymarket.impl.common.ui.PolymarketEventCardSkeleton
import com.tangem.features.polymarket.impl.common.ui.PolymarketNextPageLoader
import com.tangem.features.polymarket.impl.common.ui.PolymarketReloadPrompt
import com.tangem.features.polymarket.impl.common.feed.ui.state.PolymarketFeedFooterUM
import com.tangem.features.polymarket.impl.common.feed.ui.state.PolymarketFeedContentUM
import com.tangem.domain.polymarket.model.PolymarketEventsListConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.distinctUntilChanged

internal const val POLYMARKET_FEED_STATUS_KEY = "status"

private val ScrollIdleDebounce = 500.milliseconds

internal fun LazyListScope.polymarketEventsSection(content: PolymarketFeedContentUM, skeletonCount: Int) {
    when (content) {
        is PolymarketFeedContentUM.Loading -> items(count = skeletonCount, key = { it }) {
            PolymarketEventCardSkeleton(modifier = Modifier.padding(horizontal = 16.dp))
        }
        is PolymarketFeedContentUM.Empty -> item(key = POLYMARKET_FEED_STATUS_KEY) {
            PolymarketEmptyListPrompt(
                text = resourceReference(R.string.prediction_main_empty_events),
                modifier = Modifier.height(180.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
            )
        }
        is PolymarketFeedContentUM.Error -> item(key = POLYMARKET_FEED_STATUS_KEY) {
            PolymarketReloadPrompt(
                text = resourceReference(R.string.prediction_main_events_load_error),
                onReloadClick = content.onReloadClick,
                modifier = Modifier.height(180.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
            )
        }
        is PolymarketFeedContentUM.Content -> {
            items(
                items = content.events,
                key = { it.id },
            ) { event ->
                PolymarketEventCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    state = event,
                )
            }

            when (val footer = content.footer) {
                is PolymarketFeedFooterUM.None -> Unit
                is PolymarketFeedFooterUM.Loading -> item(key = POLYMARKET_FEED_STATUS_KEY) {
                    PolymarketNextPageLoader()
                }
                is PolymarketFeedFooterUM.Error -> item(key = POLYMARKET_FEED_STATUS_KEY) {
                    PolymarketReloadPrompt(
                        text = resourceReference(R.string.prediction_main_events_load_error),
                        onReloadClick = footer.onRetryClick,
                    )
                }
            }
        }
    }
}

@Composable
internal fun PolymarketVisibleEventsEffect(
    listState: LazyListState,
    feedKey: Any?,
    onVisibleEventsChange: (Set<String>) -> Unit,
    excludedKeys: Set<String> = setOf(POLYMARKET_FEED_STATUS_KEY),
) {
    val currentOnVisibleEventsChanged by rememberUpdatedState(onVisibleEventsChange)

    LaunchedEffect(listState, feedKey) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo
                .mapNotNull { it.key as? String }
                .filterNotTo(mutableSetOf()) { it in excludedKeys }
        }
            .distinctUntilChanged()
            .collect { visibleEventIds -> currentOnVisibleEventsChanged(visibleEventIds) }
    }
}

@Composable
internal fun PolymarketScrollIdleEffect(
    listState: LazyListState,
    feedKey: PolymarketEventsListConfig?,
    onScrollIdle: (Int, Int) -> Unit,
) {
    val currentOnScrollIdle by rememberUpdatedState(onScrollIdle)

    // Keyed by category and filter: a pending report belongs to the selection that is gone.
    LaunchedEffect(listState, feedKey) {
        snapshotFlow { listState.isScrollInProgress }
            .collectLatest { isScrollInProgress ->
                if (isScrollInProgress) return@collectLatest

                delay(ScrollIdleDebounce)
                currentOnScrollIdle(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
            }
    }
}