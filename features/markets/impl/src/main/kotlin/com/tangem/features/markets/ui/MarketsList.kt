package com.tangem.features.markets.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.buttons.SecondarySmallButton
import com.tangem.core.ui.components.buttons.SmallButtonConfig
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.components.buttons.segmentedbutton.SegmentedButtons
import com.tangem.core.ui.components.fields.SearchBar
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.markets.impl.R
import com.tangem.features.markets.model.SortByBottomSheetContentUM
import com.tangem.features.markets.ui.components.MarketsListItem
import com.tangem.features.markets.ui.components.MarketsListItemPlaceholder
import com.tangem.features.markets.ui.components.MarketsListSortByBottomSheet
import com.tangem.features.markets.ui.entity.ListUM
import com.tangem.features.markets.ui.entity.MarketsListUM
import com.tangem.features.markets.ui.entity.SortByTypeUM
import com.tangem.features.markets.ui.preview.MarketChartListItemPreviewDataProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun MarketsList(state: MarketsListUM, onHeaderSizeChange: (Dp) -> Unit, modifier: Modifier = Modifier) {
    Content(
        modifier = modifier,
        state = state,
        onHeaderSizeChange = onHeaderSizeChange,
    )

    MarketsListSortByBottomSheet(config = state.sortByBottomSheet)
}

@Composable
private fun Content(state: MarketsListUM, onHeaderSizeChange: (Dp) -> Unit, modifier: Modifier = Modifier) {
    val density = LocalDensity.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .background(color = TangemTheme.colors.background.primary),
    ) {
        SearchBar(
            state = state.searchBar,
            modifier = Modifier
                .background(color = TangemTheme.colors.background.primary)
                .padding(
                    start = TangemTheme.dimens.spacing16,
                    end = TangemTheme.dimens.spacing16,
                    bottom = TangemTheme.dimens.spacing4,
                )
                .onGloballyPositioned {
                    with(density) { onHeaderSizeChange(it.size.height.toDp()) }
                },
        )
        Spacer(Modifier.height(TangemTheme.dimens.spacing20))
        Column(Modifier.padding(horizontal = TangemTheme.dimens.size16)) {
            Title()
            SpacerH12()
            Options(
                sortByTypeUM = state.selectedSortBy,
                trendInterval = state.selectedInterval,
                onIntervalClick = state.onIntervalClick,
                onSortByClick = state.onSortByButtonClick,
            )
        }
        SpacerH12()
        Items(state = state.list)
    }
}

@Composable
private fun Title(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = stringResource(id = R.string.markets_common_title),
        style = TangemTheme.typography.h3,
        color = TangemTheme.colors.text.primary1,
    )
}

@Composable
private fun Options(
    sortByTypeUM: SortByTypeUM,
    trendInterval: MarketsListUM.TrendInterval,
    onSortByClick: () -> Unit,
    onIntervalClick: (MarketsListUM.TrendInterval) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(IntrinsicSize.Max)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        SecondarySmallButton(
            config = SmallButtonConfig(
                text = sortByTypeUM.text,
                onClick = onSortByClick,
                icon = TangemButtonIconPosition.End(iconResId = R.drawable.ic_chevron_24),
            ),
        )
        SegmentedButtons(
            config = persistentListOf(
                MarketsListUM.TrendInterval.H24,
                MarketsListUM.TrendInterval.D7,
                MarketsListUM.TrendInterval.M1,
            ),
            color = TangemTheme.colors.button.secondary,
            initialSelectedItem = trendInterval,
            onClick = onIntervalClick,
            modifier = Modifier
                .width(160.dp)
                .fillMaxHeight(),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
                    .padding(
                        vertical = TangemTheme.dimens.spacing4,
                    ),
            ) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = it.text.resolveReference(),
                    style = TangemTheme.typography.caption1,
                    color = TangemTheme.colors.text.primary1,
                )
            }
        }
    }
}

@Composable
private fun Items(state: ListUM, modifier: Modifier = Modifier) {
    val lazyListState = rememberLazyListState()
    val scrollEnabled = state !is ListUM.Loading
    val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }

    LazyColumn(
        modifier = modifier.nestedScroll(DisableParentConnection),
        state = lazyListState,
        contentPadding = PaddingValues(bottom = bottomBarHeight),
        userScrollEnabled = scrollEnabled,
    ) {
        // ATTENTION! There should be no elements with a string key value except MarketsListItem!
        when (state) {
            ListUM.Loading -> {
                items(count = 50, key = { it }) {
                    MarketsListItemPlaceholder()
                }
            }
            ListUM.SearchNothingFound -> {
                // TODO
            }
            is ListUM.Content -> {
                items(
                    items = state.items,
                    key = { it.id },
                ) { item ->
                    MarketsListItem(
                        model = item,
                    )
                }
            }
        }
    }

    LaunchedEffect(state) {
        if (state is ListUM.Loading) {
            lazyListState.scrollToItem(0)
        }
    }

    VisibleItemsTracker(lazyListState, state)

    InfiniteListHandler(
        listState = lazyListState,
        buffer = 50,
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
fun VisibleItemsTracker(listState: LazyListState, state: ListUM) {
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

//region: Preview

@Preview
@Composable
private fun Preview() {
    TangemThemePreview {
        MarketsList(
            state = MarketsListUM(
                list = ListUM.Content(
                    items = MarketChartListItemPreviewDataProvider().values
                        .flatMap { item -> List(size = 10) { item } }
                        .mapIndexed { index, item ->
                            item.copy(id = index.toString())
                        }
                        .toImmutableList(),
                    loadMore = {},
                    visibleIdsChanged = {},
                ),
                searchBar = SearchBarUM(
                    placeholderText = resourceReference(R.string.manage_tokens_search_placeholder),
                    query = "",
                    onQueryChange = {},
                    isActive = false,
                    onActiveChange = { },
                ),
                selectedSortBy = SortByTypeUM.Rating,
                selectedInterval = MarketsListUM.TrendInterval.H24,
                onIntervalClick = {},
                onSortByButtonClick = {},
                sortByBottomSheet = TangemBottomSheetConfig(
                    false,
                    onDismissRequest = {},
                    content = SortByBottomSheetContentUM(selectedOption = SortByTypeUM.Rating) {},
                ),
            ),
            onHeaderSizeChange = {},
        )
    }
}

//endregion: Preview