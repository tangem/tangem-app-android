package com.tangem.features.markets.tokenlist.impl.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.Keyboard
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.components.SpacerH8
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.buttons.SecondarySmallButton
import com.tangem.core.ui.components.buttons.SmallButtonConfig
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.components.buttons.segmentedbutton.SegmentedButtons
import com.tangem.core.ui.components.fields.SearchBar
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.components.keyboardAsState
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.markets.entry.BottomSheetState
import com.tangem.features.markets.impl.R
import com.tangem.features.markets.tokenlist.impl.ui.components.MarketsListLazyColumn
import com.tangem.features.markets.tokenlist.impl.ui.components.MarketsListSortByBottomSheet
import com.tangem.features.markets.tokenlist.impl.ui.preview.MarketChartListItemPreviewDataProvider
import com.tangem.features.markets.tokenlist.impl.ui.state.ListUM
import com.tangem.features.markets.tokenlist.impl.ui.state.MarketsListUM
import com.tangem.features.markets.tokenlist.impl.ui.state.SortByBottomSheetContentUM
import com.tangem.features.markets.tokenlist.impl.ui.state.SortByTypeUM
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun MarketsList(
    state: MarketsListUM,
    onHeaderSizeChange: (Dp) -> Unit,
    bottomSheetState: BottomSheetState,
    modifier: Modifier = Modifier,
) {
    Content(
        modifier = modifier,
        state = state,
        onHeaderSizeChange = onHeaderSizeChange,
    )
    MarketsListSortByBottomSheet(config = state.sortByBottomSheet)
    KeyboardEvents(
        isSortByBottomSheetShown = state.sortByBottomSheet.isShown,
        bottomSheetState = bottomSheetState,
    )
}

@Suppress("LongMethod")
@Composable
private fun Content(state: MarketsListUM, onHeaderSizeChange: (Dp) -> Unit, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val background = LocalMainBottomSheetColor.current.value
    val strokeColor = TangemTheme.colors.stroke.primary
    val scrolledState = remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .drawBehind { drawRect(background) },
    ) {
        SearchBar(
            modifier = Modifier
                .drawBehind { drawRect(background) }
                .padding(
                    start = TangemTheme.dimens.spacing16,
                    end = TangemTheme.dimens.spacing16,
                    bottom = 8.dp,
                )
                .onGloballyPositioned {
                    if (it.size.height > 0) {
                        with(density) {
                            onHeaderSizeChange(it.size.height.toDp())
                        }
                    }
                }
                .padding(bottom = 4.dp),
            state = state.searchBar,
        )
        Column(Modifier.padding(horizontal = TangemTheme.dimens.size16)) {
            AnimatedVisibility(
                visible = scrolledState.value.not(),
            ) {
                Column {
                    SpacerH8()
                    Title(isInSearchMode = state.isInSearchMode)
                    SpacerH12()
                }
            }
            AnimatedVisibility(state.isInSearchMode.not()) {
                Options(
                    modifier = Modifier.padding(bottom = TangemTheme.dimens.spacing12),
                    sortByTypeUM = state.selectedSortBy,
                    trendInterval = state.selectedInterval,
                    onIntervalClick = state.onIntervalClick,
                    onSortByClick = state.onSortByButtonClick,
                )
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
            scrolledState = scrolledState,
            isInSearchMode = state.isInSearchMode,
            state = state.list,
        )
    }
}

@Composable
private fun Title(isInSearchMode: Boolean, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = if (isInSearchMode) {
            stringResourceSafe(id = R.string.markets_search_result_title)
        } else {
            stringResourceSafe(id = R.string.markets_common_title)
        },
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
private fun ItemsList(
    scrolledState: MutableState<Boolean>,
    isInSearchMode: Boolean,
    state: ListUM,
    modifier: Modifier = Modifier,
) {
    val searchLazyListState = rememberLazyListState()
    val mainLazyListState = rememberLazyListState()

    val mainScrolled by remember {
        derivedStateOf {
            mainLazyListState.firstVisibleItemScrollOffset > 0
        }
    }

    val searchScrolledState by remember {
        derivedStateOf {
            searchLazyListState.firstVisibleItemScrollOffset > 0
        }
    }

    LaunchedEffect(mainScrolled, isInSearchMode, searchScrolledState) {
        scrolledState.value = if (isInSearchMode) {
            searchScrolledState
        } else {
            mainScrolled
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
private fun KeyboardEvents(isSortByBottomSheetShown: Boolean, bottomSheetState: BottomSheetState) {
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

    LaunchedEffect(bottomSheetState) {
        if (bottomSheetState == BottomSheetState.COLLAPSED) {
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
        val primaryBackground = TangemTheme.colors.background.primary

        CompositionLocalProvider(
            LocalMainBottomSheetColor provides remember { mutableStateOf(primaryBackground) },
        ) {
            MarketsList(
                state = MarketsListUM(
                    list = ListUM.Content(
                        items = MarketChartListItemPreviewDataProvider().values
                            .flatMap { item -> List(size = 10) { item } }
                            .mapIndexed { index, item ->
                                item.copy(id = index.toString())
                            }
                            .toImmutableList(),
                        showUnder100kTokensNotification = false,
                        showUnder100kTokensNotificationWasHidden = false,
                        loadMore = {},
                        visibleIdsChanged = {},
                        onShowTokensUnder100kClicked = {},
                        triggerScrollReset = consumedEvent(),
                        onItemClick = {},
                    ),
                    searchBar = SearchBarUM(
                        placeholderText = resourceReference(R.string.markets_search_header_title),
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
                        isShown = false,
                        onDismissRequest = {},
                        content = SortByBottomSheetContentUM(selectedOption = SortByTypeUM.Rating) {},
                    ),
                ),
                onHeaderSizeChange = {},
                bottomSheetState = BottomSheetState.EXPANDED,
            )
        }
    }
}

//endregion: Preview