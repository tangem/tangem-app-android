package com.tangem.features.feed.ui.market.list

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.markets.preview.MarketChartListItemPreviewDataProvider
import com.tangem.core.ui.components.*
import com.tangem.core.ui.components.appbar.AppBarWithBackButtonAndIcon
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.components.fields.SearchBar
import com.tangem.core.ui.components.fields.TangemSearchBarDefaults
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.extensions.conditionalCompose
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.model.market.list.state.*
import com.tangem.features.feed.ui.feed.state.FeedListSearchBar
import com.tangem.features.feed.ui.market.list.components.MarketsListLazyColumn
import com.tangem.features.feed.ui.market.list.components.MarketsListSortByBottomSheet
import com.tangem.features.feed.ui.market.list.components.Options
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay

private const val DELAY_FOR_FOCUS_REQUEST = 500L

@Composable
internal fun TopBarWithSearch(
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit,
    marketsSearchBar: MarketsSearchBar,
    bottomSheetState: BottomSheetState = BottomSheetState.EXPANDED,
) {
    val background = LocalMainBottomSheetColor.current.value
    val focusRequester = remember { FocusRequester() }

    val shouldShowAppBar = !marketsSearchBar.shouldAlwaysShowSearchBar &&
        !marketsSearchBar.searchBarUM.isActive &&
        marketsSearchBar.searchBarUM.query.isEmpty()

    AnimatedContent(
        targetState = shouldShowAppBar,
        label = "TopBarTransition",
    ) { showAppBar ->
        if (showAppBar) {
            AppBarWithBackButtonAndIcon(
                onBackClick = onBackClick,
                backButtonEnabled = bottomSheetState == BottomSheetState.EXPANDED,
                endButtonEnabled = bottomSheetState == BottomSheetState.EXPANDED,
                text = stringResourceSafe(R.string.markets_common_title),
                iconRes = R.drawable.ic_search_24,
                onIconClick = onSearchClick,
                backgroundColor = background,
            )
        } else {
            SearchBar(
                modifier = Modifier
                    .drawBehind { drawRect(background) }
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                state = marketsSearchBar.searchBarUM,
                colors = TangemSearchBarDefaults.defaultTextFieldColors.copy(
                    focusedContainerColor = TangemTheme.colors.field.focused,
                    unfocusedContainerColor = TangemTheme.colors.field.focused,
                ),
                focusRequester = focusRequester,
            )

            val shouldRequestFocus =
                (marketsSearchBar.shouldAlwaysShowSearchBar || marketsSearchBar.searchBarUM.isActive) &&
                    bottomSheetState == BottomSheetState.EXPANDED
            if (shouldRequestFocus) {
                LaunchedEffect(bottomSheetState) {
                    delay(DELAY_FOR_FOCUS_REQUEST)
                    focusRequester.requestFocus()
                }
            }
        }
    }
}

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
    MarketsListSortByBottomSheet(config = state.sortByBottomSheet)
    KeyboardEvents(isSortByBottomSheetShown = state.sortByBottomSheet.isShown)
}

@Suppress("LongMethod")
@Composable
private fun ColumnScope.Content(contentPadding: PaddingValues, state: MarketsListUM, modifier: Modifier = Modifier) {
    val isRedesignEnabled = LocalRedesignEnabled.current

    val hazeState = rememberHazeState()

    val strokeColor = if (isRedesignEnabled) {
        TangemTheme.colors2.border.neutral.primary
    } else {
        TangemTheme.colors.stroke.primary
    }

    val scrolledState = remember { mutableStateOf(false) }

    Column(modifier.padding(horizontal = TangemTheme.dimens.size16)) {
        SpacerH(contentPadding.calculateTopPadding())
        AnimatedVisibility(
            visible = scrolledState.value.not() &&
                state.isInSearchMode &&
                state.marketsSearchBar.searchBarUM.query.isNotEmpty(),
        ) {
            Column {
                SpacerH8()
                Text(
                    text = stringResourceSafe(id = R.string.markets_search_result_title),
                    style = TangemTheme.typography.h3,
                    color = TangemTheme.colors.text.primary1,
                )
                SpacerH12()
            }
        }
        Column {
            AnimatedVisibility(!state.isInSearchMode && !state.marketsSearchBar.shouldAlwaysShowSearchBar) {
                Options(
                    modifier = Modifier.padding(
                        bottom = if (isRedesignEnabled) {
                            TangemTheme.dimens2.x2
                        } else {
                            TangemTheme.dimens.spacing12
                        },
                    ),
                    sortByTypeUM = state.selectedSortBy,
                    trendInterval = state.selectedInterval,
                    onIntervalClick = state.onIntervalClick,
                    onSortByClick = state.onSortByButtonClick,
                    sortMenuUM = state.sortByMenuUM,
                    hazeState = hazeState,
                )
            }
        }
    }
    val strokeWidth = TangemTheme.dimens.size0_5
    Box(
        Modifier
            .fillMaxWidth()
            .height(strokeWidth)
            .drawBehind {
                // draw horizontal line
                if (scrolledState.value) {
                    drawLine(
                        color = strokeColor,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = strokeWidth.toPx(),
                    )
                }
            },
    )
    ItemsList(
        modifier = Modifier.conditionalCompose(
            condition = isRedesignEnabled,
            modifier = {
                hazeSourceTangem(zIndex = 0f, state = hazeState)
            },
        ),
        scrolledState = scrolledState,
        isInSearchMode = state.isInSearchMode,
        state = state.list,
    )
}

@Composable
private fun ItemsList(
    scrolledState: MutableState<Boolean>,
    isInSearchMode: Boolean,
    state: ListUM,
    modifier: Modifier = Modifier,
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
private fun KeyboardEvents(isSortByBottomSheetShown: Boolean) {
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

    LaunchedEffect(isSortByBottomSheetShown) {
        keyboardController?.hide()
    }
}

//region: Preview

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview() {
    TangemThemePreview(alwaysShowBottomSheets = false) {
        val primaryBackground = TangemTheme.colors.background.primary

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
                    onSortByButtonClick = {},
                    sortByBottomSheet = TangemBottomSheetConfig(
                        isShown = false,
                        onDismissRequest = {},
                        content = SortByBottomSheetContentUM(selectedOption = SortByTypeUM.Rating) {},
                    ),
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