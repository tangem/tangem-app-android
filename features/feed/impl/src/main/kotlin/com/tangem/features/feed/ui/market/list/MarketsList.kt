package com.tangem.features.feed.ui.market.list

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.markets.preview.MarketChartListItemPreviewDataProvider
import com.tangem.core.ui.components.Keyboard
import com.tangem.core.ui.components.TopFade
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.components.keyboardAsState
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.model.market.list.state.*
import com.tangem.features.feed.ui.feed.state.FeedListSearchBar
import com.tangem.features.feed.ui.feedTopFadeColorStops
import com.tangem.features.feed.ui.market.list.components.MarketsListLazyColumn
import com.tangem.features.feed.ui.market.list.components.Options
import com.tangem.features.feed.ui.utils.FadeConstants.BASE_FADE_LEVEL
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun MarketsList(contentPadding: PaddingValues, state: MarketsListUM, modifier: Modifier = Modifier) {
    val background = LocalMainBottomSheetColor.current.value
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .drawBehind { drawRect(background) },
    ) {
        Content(state = state, contentPadding = contentPadding)
    }
    KeyboardEvents()
}

@Suppress("LongMethod")
@Composable
private fun ColumnScope.Content(contentPadding: PaddingValues, state: MarketsListUM) {
    val scrolledState = remember { mutableStateOf(false) }
    var optionsHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    val fadeColor = TangemTheme.colors3.bg.primary.copy(BASE_FADE_LEVEL)
    val topPadding = contentPadding.calculateTopPadding()

    Box(modifier = Modifier.fillMaxSize()) {
        ItemsList(
            modifier = Modifier.align(Alignment.TopStart),
            topContentPadding = topPadding + optionsHeight,
            scrolledState = scrolledState,
            isInSearchMode = state.isInSearchMode,
            state = state.list,
        )
        TopFade(
            modifier = Modifier.padding(top = topPadding),
            colorStops = feedTopFadeColorStops(fadeColor),
            height = 16.dp + optionsHeight,
        )
        Options(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(bottom = 16.dp, top = topPadding)
                .padding(horizontal = 16.dp)
                .onGloballyPositioned { coordinates ->
                    if (coordinates.size.height > 0) {
                        with(density) {
                            optionsHeight = coordinates.size.height.toDp()
                        }
                    }
                },
            trendInterval = state.selectedInterval,
            onIntervalClick = state.onIntervalClick,
            sortMenuUM = state.sortByMenuUM,
        )
    }
}

@Composable
private fun ItemsList(
    scrolledState: MutableState<Boolean>,
    isInSearchMode: Boolean,
    state: ListUM,
    modifier: Modifier = Modifier,
    topContentPadding: Dp = 0.dp,
) {
    val searchLazyListState = rememberLazyListState()
    val mainLazyListState = rememberLazyListState()

    val isMainScrolled by remember {
        derivedStateOf {
            mainLazyListState.firstVisibleItemScrollOffset > 0
        }
    }

    val isSearchScrolled by remember {
        derivedStateOf {
            searchLazyListState.firstVisibleItemScrollOffset > 0
        }
    }

    LaunchedEffect(isMainScrolled, isInSearchMode, isSearchScrolled) {
        scrolledState.value = if (isInSearchMode) {
            isSearchScrolled
        } else {
            isMainScrolled
        }
    }

    MarketsListLazyColumn(
        topContentPadding = topContentPadding,
        modifier = modifier,
        state = state,
        isInSearchMode = isInSearchMode,
        lazyListState = if (isInSearchMode) {
            searchLazyListState
        } else {
            mainLazyListState
        },
    )
}

@Composable
private fun KeyboardEvents() {
    val keyboardController = LocalSoftwareKeyboardController.current
    val keyboard by keyboardAsState()
    val focusManager = LocalFocusManager.current

    BackHandler(enabled = keyboard is Keyboard.Opened) {
        keyboardController?.hide()
    }

    LaunchedEffect(keyboard) {
        if (keyboard is Keyboard.Closed) {
            focusManager.clearFocus()
        }
    }
}

//region: Preview

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview() {
    TangemThemePreview(alwaysShowBottomSheets = false) {
        val primaryBackground = TangemTheme.colors3.bg.primary

        CompositionLocalProvider(
            LocalMainBottomSheetColor provides remember { mutableStateOf(primaryBackground) },
        ) {
            MarketsList(
                contentPadding = PaddingValues(),
                state = MarketsListUM(
                    list = ListUM.Content(
                        items = MarketChartListItemPreviewDataProvider().values
                            .flatMap { item -> List(size = 10) { item } }
                            .mapIndexed { index, item ->
                                item.copy(id = CryptoCurrency.RawID(index.toString()))
                            }
                            .toImmutableList(),
                        shouldShowUnder100kTokensNotification = false,
                        shouldShowUnder100kTokensNotificationWasHidden = false,
                        loadMore = {},
                        visibleIdsChanged = {},
                        onShowTokensUnder100kClicked = {},
                        triggerScrollReset = consumedEvent(),
                        onItemClick = {},
                    ),
                    marketsSearchBar = MarketsSearchBar(
                        searchBarUM = SearchBarUM(
                            placeholderText = resourceReference(R.string.markets_search_header_title),
                            query = "",
                            onQueryChange = {},
                            isActive = false,
                            onActiveChange = { },
                        ),
                        shouldAlwaysShowSearchBar = true,
                    ),
                    selectedSortBy = SortByTypeUM.Rating,
                    selectedInterval = MarketsListUM.TrendInterval.H24,
                    onIntervalClick = {},
                    onSearchClicked = {},
                    feedListSearchBar = FeedListSearchBar(
                        onBarClick = {},
                        placeholderText = resourceReference(id = R.string.markets_search_title_placeholder),
                    ),
                    sortByMenuUM = SortByMenuUM(
                        selectedOption = SortByTypeUM.Rating,
                        onOptionClicked = {},
                    ),
                ),
            )
        }
    }
}

//endregion: Preview