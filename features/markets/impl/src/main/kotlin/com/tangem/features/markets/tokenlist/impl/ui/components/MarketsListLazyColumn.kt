package com.tangem.features.markets.tokenlist.impl.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import com.tangem.core.ui.components.buttons.SecondarySmallButton
import com.tangem.core.ui.components.buttons.SmallButtonConfig
import com.tangem.core.ui.event.EventEffect
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.markets.impl.R
import com.tangem.features.markets.tokenlist.impl.ui.entity.ListUM
import kotlinx.coroutines.launch

private const val LOAD_NEXT_PAGE_ON_END_INDEX = 50

@Composable
@Suppress("LongMethod")
internal fun MarketsListLazyColumn(
    state: ListUM,
    isInSearchMode: Boolean,
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }
    val coroutineScope = rememberCoroutineScope()

    SideEffect {
        if (state is ListUM.Loading) {
            coroutineScope.launch {
                lazyListState.scrollToItem(0)
            }
        }
    }

    if (state is ListUM.Content) {
        EventEffect(state.triggerScrollReset) {
            lazyListState.scrollToItem(0)
        }
    }

    if (state is ListUM.Loading) {
        LazyColumn(
            modifier = Modifier.nestedScroll(DisableParentConnection),
            state = rememberLazyListState(),
            contentPadding = PaddingValues(bottom = bottomBarHeight),
            userScrollEnabled = false,
        ) {
            items(count = 100, key = { it }) {
                MarketsListItemPlaceholder()
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.nestedScroll(DisableParentConnection),
            state = lazyListState,
            contentPadding = PaddingValues(bottom = bottomBarHeight),
            userScrollEnabled = true,
        ) {
            // ATTENTION! There should be no elements with a string key value except MarketsListItem!
            when (state) {
                is ListUM.LoadingError -> {
                    item(key = "loading error".hashCode()) {
                        LoadingErrorItem(
                            modifier = Modifier.fillParentMaxSize(),
                            onTryAgain = state.onRetryClicked,
                        )
                    }
                }
                ListUM.SearchNothingFound -> {
                    item(key = "not found text".hashCode()) {
                        SearchNothingFoundText(
                            modifier = Modifier.fillParentMaxSize(),
                        )
                    }
                }
                is ListUM.Content -> {
                    items(
                        items = state.items,
                        key = { it.id },
                    ) { item ->
                        MarketsListItem(
                            model = item,
                            onClick = { state.onItemClick(item) },
                        )
                    }

                    if (isInSearchMode && state.showUnder100kTokens.not()) {
                        item(key = "show tokens under 100k".hashCode()) {
                            ShowTokensUnder100kItem(
                                onShowTokensClick = state.onShowTokensUnder100kClicked,
                            )
                        }
                    }
                }
                else -> {}
            }
        }
    }

    VisibleItemsTracker(lazyListState, state)

    InfiniteListHandler(
        listState = lazyListState,
        buffer = LOAD_NEXT_PAGE_ON_END_INDEX,
        onLoadMore = remember(state) {
            {
                if (state is ListUM.Content) {
                    state.loadMore()
                }
            }
        },
    )
}

@Composable
private fun LoadingErrorItem(onTryAgain: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .padding(
                horizontal = TangemTheme.dimens.spacing16,
                vertical = TangemTheme.dimens.spacing12,
            )
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        UnableToLoadData(onRetryClick = onTryAgain)
    }
}

@Composable
private fun ShowTokensUnder100kItem(onShowTokensClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(
                horizontal = TangemTheme.dimens.spacing16,
                vertical = TangemTheme.dimens.spacing12,
            )
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.markets_search_see_tokens_under_100k),
            style = TangemTheme.typography.caption1,
            color = TangemTheme.colors.text.tertiary,
        )
        SecondarySmallButton(
            config = SmallButtonConfig(
                text = resourceReference(R.string.markets_search_show_tokens),
                onClick = onShowTokensClick,
            ),
        )
    }
}

@Composable
private fun SearchNothingFoundText(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.markets_search_token_no_result_title),
            style = TangemTheme.typography.caption1,
            color = TangemTheme.colors.text.tertiary,
        )
    }
}

@Composable
private fun VisibleItemsTracker(listState: LazyListState, state: ListUM) {
    val visibleItems by remember {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? String }
        }
    }

    LaunchedEffect(listState.isScrollInProgress, visibleItems) {
        if (state is ListUM.Content && listState.isScrollInProgress.not()) {
            state.visibleIdsChanged(visibleItems)
        }
    }
}

@Composable
fun InfiniteListHandler(listState: LazyListState, onLoadMore: () -> Unit, buffer: Int = 2) {
    val loadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1

            lastVisibleItemIndex > totalItemsNumber - buffer
        }
    }

    val totalItemsCount by remember { derivedStateOf { listState.layoutInfo.totalItemsCount } }
    var emitted by remember(totalItemsCount) { mutableStateOf(false) }

    LaunchedEffect(loadMore) {
        if (loadMore && !emitted) {
            emitted = true
            onLoadMore()
        }
    }
}

private object DisableParentConnection : NestedScrollConnection {
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        return available.copy(x = 0f)
    }
}