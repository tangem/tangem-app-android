package com.tangem.features.markets.tokenlist.impl.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.Keyboard
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.components.SpacerH8
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.components.buttons.SecondarySmallButton
import com.tangem.core.ui.components.buttons.SmallButtonConfig
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.components.buttons.segmentedbutton.SegmentedButtons
import com.tangem.core.ui.components.fields.SearchBar
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.components.keyboardAsState
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.markets.impl.R
import com.tangem.features.markets.tokenlist.impl.model.MarketsNotificationUM
import com.tangem.features.markets.tokenlist.impl.ui.components.MarketsListLazyColumn
import com.tangem.features.markets.tokenlist.impl.ui.components.MarketsListSortByBottomSheet
import com.tangem.features.markets.tokenlist.impl.ui.components.YieldSupplyInMarketsPromoNotification
import com.tangem.features.markets.tokenlist.impl.ui.preview.MarketChartListItemPreviewDataProvider
import com.tangem.features.markets.tokenlist.impl.ui.state.ListUM
import com.tangem.features.markets.tokenlist.impl.ui.state.MarketsListUM
import com.tangem.features.markets.tokenlist.impl.ui.state.SortByBottomSheetContentUM
import com.tangem.features.markets.tokenlist.impl.ui.state.SortByTypeUM
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

private const val SHOW_MORE_KEY = "privacyPolicy"

@Composable
internal fun MarketsList(
    state: MarketsListUM,
    onHeaderSizeChange: (Dp) -> Unit,
    bottomSheetState: BottomSheetState,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val background = LocalMainBottomSheetColor.current.value
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
                .onGloballyPositioned { layoutCoordinates ->
                    if (layoutCoordinates.size.height > 0) {
                        with(density) {
                            onHeaderSizeChange(layoutCoordinates.size.height.toDp())
                        }
                    }
                }
                .padding(bottom = 4.dp),
            state = state.searchBar,
        )
        Content(state = state)
    }
    MarketsListSortByBottomSheet(config = state.sortByBottomSheet)
    KeyboardEvents(
        isSortByBottomSheetShown = state.sortByBottomSheet.isShown,
        bottomSheetState = bottomSheetState,
    )
}

@Composable
internal fun MarketsListWithBack(
    state: MarketsListUM,
    bottomSheetState: BottomSheetState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = LocalMainBottomSheetColor.current.value
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .drawBehind { drawRect(background) },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = rememberVectorPainter(
                    ImageVector.vectorResource(R.drawable.ic_close_24),
                ),
                contentDescription = null,
                tint = TangemTheme.colors.icon.primary1,
                modifier = Modifier
                    .padding(16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false),
                        onClick = onBackClick,
                    ),
            )
            SearchBar(
                modifier = Modifier
                    .drawBehind { drawRect(background) }
                    .padding(
                        end = 16.dp,
                    ),
                state = state.searchBar,
            )
        }
        Content(state = state)
    }
    MarketsListSortByBottomSheet(config = state.sortByBottomSheet)
    KeyboardEvents(
        isSortByBottomSheetShown = state.sortByBottomSheet.isShown,
        bottomSheetState = bottomSheetState,
    )
}

@Suppress("LongMethod")
@Composable
private fun ColumnScope.Content(state: MarketsListUM, modifier: Modifier = Modifier) {
    val strokeColor = TangemTheme.colors.stroke.primary
    val scrolledState = remember { mutableStateOf(false) }

    Column(modifier.padding(horizontal = TangemTheme.dimens.size16)) {
        AnimatedVisibility(
            visible = scrolledState.value.not(),
        ) {
            Column {
                SpacerH8()
                Title(isInSearchMode = state.isInSearchMode)
                SpacerH12()
            }
        }
        Column {
            AnimatedVisibility(state.isInSearchMode.not()) {
                Options(
                    modifier = Modifier.padding(bottom = TangemTheme.dimens.spacing12),
                    sortByTypeUM = state.selectedSortBy,
                    trendInterval = state.selectedInterval,
                    onIntervalClick = state.onIntervalClick,
                    onSortByClick = state.onSortByButtonClick,
                )
            }

            val marketsNotificationUM = state.marketsNotificationUM
            AnimatedVisibility(
                state.list !is ListUM.LoadingError &&
                    state.isInSearchMode.not() && state.selectedSortBy != SortByTypeUM.YieldSupply,
            ) {
                val showMore = stringResourceSafe(R.string.common_show_more)

                when (marketsNotificationUM) {
                    is MarketsNotificationUM.YieldSupplyPromo -> {
                        val description = stringResourceSafe(
                            R.string.markets_yield_supply_banner_description,
                            showMore,
                        )

                        val clickableDescription = annotatedReference {
                            append(description.substringBefore(showMore))

                            pushStringAnnotation(SHOW_MORE_KEY, "")
                            appendColored(showMore, TangemTheme.colors.text.accent)
                            pop()
                        }

                        YieldSupplyInMarketsPromoNotification(
                            config = marketsNotificationUM.config.copy(
                                subtitle = clickableDescription,
                            ),
                            modifier = Modifier.padding(bottom = TangemTheme.dimens.spacing12),
                        )
                    }
                    else -> { /* no-op */
                    }
                }
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
        scrolledState = scrolledState,
        isInSearchMode = state.isInSearchMode,
        state = state.list,
    )
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
                                item.copy(id = CryptoCurrency.RawID(index.toString()))
                            }
                            .toImmutableList(),
                        shouldShowUnder100kTokensNotification = false,
                        wasUnder100kTokensNotificationHidden = false,
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
                    marketsNotificationUM = MarketsNotificationUM.YieldSupplyPromo(
                        onClick = {},
                        onCloseClick = {},
                    ),
                ),
                onHeaderSizeChange = {},
                bottomSheetState = BottomSheetState.EXPANDED,
            )
        }
    }
}

//endregion: Preview