package com.tangem.feature.wallet.presentation.wallet.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.common.ui.notifications.notifications
import com.tangem.common.ui.notifications.stackedNotifications
import com.tangem.core.ui.components.atoms.Hand
import com.tangem.core.ui.components.atoms.handComposableComponentHeight
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.components.containers.pullToRefresh.TangemPullToRefreshContainer
import com.tangem.core.ui.components.haze.hazeEffectTangem
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.components.rememberIsKeyboardVisible
import com.tangem.core.ui.components.sheetscaffold.*
import com.tangem.core.ui.components.snackbar.CopiedTextSnackbar
import com.tangem.core.ui.components.snackbar.TangemSnackbar
import com.tangem.core.ui.decompose.ComposableStatelessListContentComponent
import com.tangem.core.ui.ds.TangemPagerIndicator
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.event.StateEvent
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.*
import com.tangem.core.ui.test.MainScreenTestTags
import com.tangem.core.ui.utils.toPx
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.preview.WalletScreenPreviewData.accountScreenState
import com.tangem.feature.wallet.presentation.common.preview.WalletScreenPreviewData.accountScreenWithEmptyTokensState
import com.tangem.feature.wallet.presentation.common.preview.WalletScreenPreviewData.walletScreenState
import com.tangem.feature.wallet.presentation.wallet.state.model.NOT_INITIALIZED_WALLET_INDEX
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletEvent
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletScreenState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletUM
import com.tangem.feature.wallet.presentation.wallet.ui.components.*
import com.tangem.feature.wallet.presentation.wallet.ui.components.common.WalletBalance
import com.tangem.feature.wallet.presentation.wallet.ui.components.multicurrency.tokensListItems2
import dev.chrisbanes.haze.HazeProgressive
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.roundToInt

@Composable
internal fun WalletScreen2(
    state: WalletScreenState,
    nftEntryBlockComponent: ComposableStatelessListContentComponent,
    bottomSheetContent: @Composable (() -> Unit),
    bottomSheetHeaderHeightProvider: () -> Dp,
    onBottomSheetStateChange: (BottomSheetState) -> Unit,
) {
    // It means that screen is still initializing
    if (state.selectedWalletIndex == NOT_INITIALIZED_WALLET_INDEX) return

    val walletsListState = rememberLazyListState(initialFirstVisibleItemIndex = state.selectedWalletIndex)
    val snackbarHostState = remember(::SnackbarHostState)
    val isAutoScroll = remember { mutableStateOf(value = false) }

    WalletContent2(
        state = state,
        nftEntryBlockComponent = nftEntryBlockComponent,
        walletsListState = walletsListState,
        snackbarHostState = snackbarHostState,
        isAutoScroll = isAutoScroll,
        onAutoScrollReset = { isAutoScroll.value = false },
        bottomSheetContent = bottomSheetContent,
        bottomSheetHeaderHeightProvider = bottomSheetHeaderHeightProvider,
        onBottomSheetStateChange = onBottomSheetStateChange,
    )

    WalletEventEffect(
        walletsListState = walletsListState,
        snackbarHostState = snackbarHostState,
        event = state.event,
        onAutoScrollSet = { isAutoScroll.value = true },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod", "LongParameterList", "CyclomaticComplexMethod")
@Composable
private fun WalletContent2(
    state: WalletScreenState,
    nftEntryBlockComponent: ComposableStatelessListContentComponent,
    walletsListState: LazyListState,
    snackbarHostState: SnackbarHostState,
    isAutoScroll: State<Boolean>,
    onAutoScrollReset: () -> Unit,
    bottomSheetHeaderHeightProvider: () -> Dp,
    onBottomSheetStateChange: (BottomSheetState) -> Unit,
    bottomSheetContent: @Composable (() -> Unit),
) {
    /*
     * Don't pass key to remember, because it will brake scroll animation.
     * selectedWalletIndex will be changed in WalletsListEffects.
     */
    val selectedWalletIndex by remember(state.selectedWalletIndex) { mutableIntStateOf(state.selectedWalletIndex) }
    val selectedWallet = state.wallets2.getOrElse(selectedWalletIndex) { state.wallets2[state.selectedWalletIndex] }

    val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }
    val statusBarHeight = with(LocalDensity.current) { WindowInsets.statusBars.getBottom(this).toDp() }

    val listState = rememberLazyListState()

    val partialCollapsedHeight = 64.dp + statusBarHeight
    val balanceBlockHeight = 296.dp + partialCollapsedHeight
    val topBarState = rememberWalletBalanceState(
        heightOffsetLimit = -balanceBlockHeight.toPx(),
        partialHeightLimit = partialCollapsedHeight.toPx(),
    )
    val behavior = customExitUntilCollapsedScrollBehavior(
        state = topBarState,
        snapAnimationSpec = spring(stiffness = Spring.StiffnessMedium),
    )

    val scaffoldContent: @Composable (PaddingValues?) -> Unit = { paddingValues ->
        val movableItemModifier = Modifier.padding(horizontal = TangemTheme.dimens2.x3)

        val itemModifier = movableItemModifier
            .padding(top = TangemTheme.dimens2.x3)

        val marketHintAproxHeight = with(LocalDensity.current) {
            TangemTheme.typography.caption2.lineHeight.toDp() * 2
        } + 40.dp

        val contentPadding = paddingValues?.let { padding ->
            PaddingValues(
                bottom = padding.calculateBottomPadding() + marketHintAproxHeight + 52.dp,
            )
        } ?: PaddingValues(bottom = TangemTheme.dimens.spacing92 + bottomBarHeight)

        val containerColor = TangemTheme.colors2.surface.level1

        val pagerState = rememberPagerState(
            initialPage = selectedWalletIndex,
            pageCount = { state.wallets2.size },
        )

        val canPagerScroll by remember {
            derivedStateOf {
                behavior.state.heightOffset == 0f
            }
        }

        val collapsedFraction = behavior.state.collapsedFraction
        val alpha = 1f - collapsedFraction
        val scale = alpha.coerceIn(0.75f, 1f)

        Box {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = canPagerScroll,
            ) {
                TopBar(
                    state = behavior.state,
                    collapsingPart = {
                        WalletBalance(
                            walletBalanceUM = selectedWallet.walletsBalanceUM,
                            buttons = selectedWallet.buttons,
                            isBalanceHidden = state.isHidingMode,
                            modifier = Modifier
                                .alpha(alpha)
                                .scale(scale)
                                .draggable(
                                    orientation = Orientation.Vertical,
                                    state = rememberDraggableState { delta ->
                                        behavior.state.heightOffset += delta
                                    },
                                    onDragStopped = { velocity ->
                                        settleAppBar(
                                            state = behavior.state,
                                            velocity = velocity,
                                            flingAnimationSpec = behavior.flingAnimationSpec,
                                            snapAnimationSpec = behavior.snapAnimationSpec,
                                        )
                                    },
                                )
                        )
                    },
                    body = {
                        LazyColumn(
                            modifier = Modifier
                                .testTag(MainScreenTestTags.SCREEN_CONTAINER),
                            state = listState,
                            contentPadding = contentPadding,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            notifications(
                                notifications = selectedWallet.notifications.map { it.messageUM }.toPersistentList(),
                                contentColor = containerColor,
                                modifier = movableItemModifier,
                            )

                            stackedNotifications(
                                contentColor = containerColor,
                                modifier = movableItemModifier,
                                notifications = (selectedWallet as? WalletUM.Content)?.stackableNotifications
                                    ?.map { it.messageUM }
                                    ?.toPersistentList(),
                            )

                            tangemPay(
                                walletUM = selectedWallet,
                                isBalanceHiding = state.isHidingMode,
                                modifier = itemModifier,
                            )

                            tokensListItems2(
                                walletTokensListUM = selectedWallet.tokensListUM,
                                modifier = movableItemModifier,
                                isBalanceHidden = state.isHidingMode,
                            )

                            with(nftEntryBlockComponent) {
                                content(itemModifier)
                            }

                            organizeTokens2(state = selectedWallet, itemModifier = itemModifier)
                        }
                    }
                )
            }
            TangemPagerIndicator(
                pagerState = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scale)
                    .offset {
                        IntOffset(
                            x = 0,
                            y = balanceBlockHeight.roundToPx() + behavior.state.heightOffset.roundToInt()
                        )
                    }
            )
        }

        WalletsListEffects(
            lazyListState = walletsListState,
            selectedWalletIndex = selectedWalletIndex,
            onUserScroll = onAutoScrollReset,
            onIndexChange = { index ->
                // Auto scroll must not change wallet
                if (isAutoScroll.value) {
                    state.onWalletChange(index, true)
                } else {
                    state.onWalletChange(index, false)
                }
            },
        )
    }

    TangemPullToRefreshContainer(
        config = selectedWallet.pullToRefreshConfig,
        indicatorModifier = Modifier.padding(top = partialCollapsedHeight)
    ) {
        BaseScaffoldWithMarkets(
            state = state,
            listState = listState,
            snackbarHostState = snackbarHostState,
            bottomSheetHeaderHeightProvider = bottomSheetHeaderHeightProvider,
            onBottomSheetStateChange = onBottomSheetStateChange,
            bottomSheetContent = bottomSheetContent,
            content = scaffoldContent,
            modifier = Modifier.nestedScroll(behavior.nestedScrollConnection),
        )
    }
}

@Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private inline fun BaseScaffoldWithMarkets(
    state: WalletScreenState,
    snackbarHostState: SnackbarHostState,
    listState: LazyListState,
    bottomSheetHeaderHeightProvider: () -> Dp,
    modifier: Modifier = Modifier,
    noinline onBottomSheetStateChange: (BottomSheetState) -> Unit,
    crossinline bottomSheetContent: @Composable () -> Unit,
    crossinline content: @Composable (PaddingValues) -> Unit,
) {
    val bottomSheetState = rememberTangemStandardBottomSheetState()
    val isPowerSaving by LocalPowerSavingState.current.isPowerSavingModeEnabled.collectAsStateWithLifecycle()

    val isKeyboardVisible by rememberIsKeyboardVisible()

    val scaffoldState = rememberTangemBottomSheetScaffoldState(
        bottomSheetState = bottomSheetState,
        snackbarHostState = snackbarHostState,
    )

    val density = LocalDensity.current
    val bottomBarHeight = with(density) { WindowInsets.systemBars.getBottom(density = this).toDp() }
    val statusBarHeight = with(density) { WindowInsets.statusBars.getTop(density = this).toDp() }
    val peekHeight = bottomSheetHeaderHeightProvider() + handComposableComponentHeight + bottomBarHeight
    val maxHeight = LocalWindowSize.current.height

    val coroutineScope = rememberCoroutineScope()
    val background = if (state.isNewMarketEnabled) {
        TangemTheme.colors.background.tertiary
    } else {
        TangemTheme.colors.background.primary
    }

    val isShowMarketsHint by remember {
        derivedStateOf {
            // Show hint only when there are items in the list
            // and when there a no items to scroll
            listState.layoutInfo.totalItemsCount > 0 &&
                !listState.canScrollBackward && !listState.canScrollForward ||
                listState.canScrollBackward && !listState.canScrollForward
        }
    }

    CompositionLocalProvider(
        LocalMainBottomSheetColor provides remember(background) { mutableStateOf(background) },
    ) {
        val backgroundColor = LocalMainBottomSheetColor.current
        var isSearchFieldFocused by remember { mutableStateOf(false) }
        val isNavBarVisible = remember { mutableStateOf(true) }

        BottomSheetStateEffects(
            bottomSheetState = bottomSheetState,
            onBottomSheetStateChange = onBottomSheetStateChange,
            navigationBarVisible = isNavBarVisible,
            isSearchFieldFocused = isSearchFieldFocused,
        )

        Box {
            TangemBottomSheetScaffold(
                modifier = modifier,
                snackbarHost = { snackbarHostState ->
                    WalletSnackbarHost(
                        snackbarHostState = snackbarHostState,
                        event = state.event,
                        modifier = Modifier
                            .padding(bottom = TangemTheme.dimens.spacing4)
                            .navigationBarsPadding(),
                    )
                },
                containerColor = TangemTheme.colors2.surface.level1,
                sheetContainerColor = backgroundColor.value,
                scaffoldState = scaffoldState,
                sheetPeekHeight = peekHeight,
                sheetShape = TangemTheme.shapes.bottomSheetLarge,
                sheetContent = {
                    // hide bottom sheet when back pressed
                    BackHandler(
                        isKeyboardVisible.not() &&
                            bottomSheetState.currentValue == TangemSheetValue.Expanded,
                    ) {
                        coroutineScope.launch { bottomSheetState.partialExpand() }
                    }

                    Column(
                        modifier = Modifier
                            // expand bottom sheet when clicked on the header
                            .clickable(
                                enabled = bottomSheetState.currentValue == TangemSheetValue.PartiallyExpanded,
                                indication = null,
                                interactionSource = null,
                            ) {
                                coroutineScope.launch { bottomSheetState.expand() }
                            }
                            .sizeIn(maxHeight = maxHeight - statusBarHeight),
                    ) {
                        Hand(Modifier.drawBehind { drawRect(backgroundColor.value) })

                        Box(
                            modifier = Modifier
                                .onFocusChanged {
                                    isSearchFieldFocused = it.isFocused
                                },
                        ) {
                            bottomSheetContent()
                        }
                    }
                },
                content = { paddingValues ->
                    Box {
                        MarketsHint2(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = peekHeight + 12.dp)
                                .fillMaxWidth(fraction = .4f),
                            isVisible = isShowMarketsHint,
                        )

                        Column(
                            modifier = Modifier.hazeSourceTangem(-1f),
                        ) {
                            content(paddingValues)
                        }

                        Surface(
                            color = Color.Unspecified,
                            contentColor = Color.Unspecified,
                            modifier = Modifier
                                .hazeEffectTangem {
                                    progressive =
                                        HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f)
                                },
                        ) {
                            TangemTopBar(
                                title = stringReference(""), // todo balance
                                startIconRes = R.drawable.ic_tangem_24,
                                endIconRes = R.drawable.ic_more_default_24,
                                onEndContentClick = state.topBarConfig.onDetailsClick,
                                isGhostButtons = !isPowerSaving,
                                modifier = Modifier
                                    .testTag(MainScreenTestTags.TOP_BAR),
                            )
                        }

                        BottomSheetScrim(
                            color = if (state.showMarketsOnboarding) {
                                Color.Black.copy(alpha = .65f)
                            } else {
                                Color.Black.copy(alpha = .40f)
                            },
                            visible = bottomSheetState.targetValue == TangemSheetValue.Expanded ||
                                state.showMarketsOnboarding,
                            onDismissRequest = {
                                coroutineScope.launch { bottomSheetState.partialExpand() }
                                state.onDismissMarketsTooltip()
                            },
                        )

                        MarketsTooltip(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 24.dp)
                                .fillMaxWidth(fraction = 0.7f),
                            isVisible = state.showMarketsOnboarding,
                            availableHeight = maxHeight,
                            bottomSheetState = bottomSheetState,
                        )
                    }
                },
            )

            AnimatedVisibility(
                modifier = Modifier.align(Alignment.BottomCenter),
                visible = isNavBarVisible.value,
            ) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .background(backgroundColor.value)
                        .height(bottomBarHeight)
                        .fillMaxWidth(),
                )
            }
        }

        LaunchedEffect(state.showMarketsOnboarding, bottomSheetState.targetValue) {
            if (state.showMarketsOnboarding && bottomSheetState.targetValue == TangemSheetValue.Expanded) {
                state.onDismissMarketsTooltip()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    state: WalletBalanceScrollState,
    collapsingPart: @Composable () -> Unit,
    body: @Composable () -> Unit,
) {
    Layout(
        content = {
            collapsingPart()
            body()
        },
    ) { measurables, constraints ->

        val balancePlaceable = measurables[0].measure(constraints)
        val bodyPlaceable = measurables[1].measure(constraints)

        val minHeight = 0.dp.roundToPx()
        val maxHeight = balancePlaceable.height + minHeight

        val offset = state.heightOffset.roundToInt().coerceAtLeast(-maxHeight)

        val height = (maxHeight + offset).coerceIn(minHeight, maxHeight)

        Timber.d(
            "TOPBAR: maxHeight = $maxHeight, result height = $height " +
                "heightOffset = ${state.heightOffset}, fixedHeightOffset = $offset, heightOffsetLimit = ${state.heightOffsetLimit}"
        )

        layout(width = constraints.maxWidth, height = constraints.maxHeight) {
            balancePlaceable.placeRelative(0, offset)
            bodyPlaceable.placeRelative(0, balancePlaceable.height + offset)
        }
    }
}

@Composable
private fun BottomSheetScrim(color: Color, visible: Boolean, onDismissRequest: () -> Unit) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(),
        label = "scrim",
    )
    val dismissSheet = if (visible) {
        Modifier
            .pointerInput(onDismissRequest) {
                detectTapGestures {
                    onDismissRequest()
                }
            }
            .clearAndSetSemantics {}
    } else {
        Modifier
    }
    Canvas(
        Modifier
            .fillMaxSize()
            .then(dismissSheet),
    ) {
        drawRect(color = color, alpha = alpha)
    }
}

@Suppress("CyclomaticComplexMethod", "MagicNumber", "LongMethod")
@Composable
private fun BottomSheetStateEffects(
    bottomSheetState: TangemSheetState,
    navigationBarVisible: MutableState<Boolean>,
    onBottomSheetStateChange: (BottomSheetState) -> Unit,
    isSearchFieldFocused: Boolean,
) {
    LaunchedEffect(bottomSheetState.targetValue) {
        when (bottomSheetState.targetValue) {
            TangemSheetValue.Hidden,
            TangemSheetValue.Expanded,
            -> navigationBarVisible.value = false
            TangemSheetValue.PartiallyExpanded,
            -> navigationBarVisible.value = true
        }
    }

    // expand bottom sheet when keyboard appears
    val isKeyboardVisible by rememberIsKeyboardVisible()

    LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible && isSearchFieldFocused) {
            bottomSheetState.expand()
        }
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    // hide keyboard when bottom sheet is about to be hidden
    LaunchedEffect(Unit) {
        snapshotFlow {
            bottomSheetState.currentValue == TangemSheetValue.Expanded &&
                bottomSheetState.targetValue == TangemSheetValue.PartiallyExpanded
        }.collect { sheetHasBeenHidden ->
            if (sheetHasBeenHidden) {
                keyboardController?.hide()
            }
        }
    }

    val isSheetHidden = bottomSheetState.targetValue == TangemSheetValue.PartiallyExpanded
    LaunchedEffect(isSheetHidden) {
        onBottomSheetStateChange(
            if (isSheetHidden) {
                BottomSheetState.COLLAPSED
            } else {
                BottomSheetState.EXPANDED
            },
        )
    }
}

@Composable
private fun WalletSnackbarHost(
    snackbarHostState: SnackbarHostState,
    event: StateEvent<WalletEvent>,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(hostState = snackbarHostState, modifier = modifier) { data ->
        if (event is StateEvent.Triggered && event.data is WalletEvent.CopyAddress) {
            CopiedTextSnackbar(data)
        } else {
            TangemSnackbar(data)
        }
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WalletScreen2_Preview(@PreviewParameter(WalletScreen2PreviewProvider::class) data: WalletScreenState) {
    TangemThemePreviewRedesign {
        WalletScreen2(
            state = data,
            nftEntryBlockComponent = ComposableStatelessListContentComponent.EMPTY,
            bottomSheetContent = {
                Text("Markets Content")
            },
            bottomSheetHeaderHeightProvider = { 10.dp },
            onBottomSheetStateChange = {},
        )
    }
}

private class WalletScreen2PreviewProvider : PreviewParameterProvider<WalletScreenState> {
    override val values: Sequence<WalletScreenState>
        get() = sequenceOf(
            walletScreenState,
            walletScreenState.copy(selectedWalletIndex = 1),
            accountScreenState.copy(selectedWalletIndex = 1),
            accountScreenWithEmptyTokensState.copy(selectedWalletIndex = 1),
        )
}
// endregion