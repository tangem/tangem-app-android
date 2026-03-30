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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.tangem.core.ui.components.BottomFade
import com.tangem.core.ui.components.atoms.handComposableComponentHeight
import com.tangem.core.ui.components.background.northernlights.NorthernLightsBackground
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheetDraggableHeader
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.components.containers.pullToRefresh.TangemPullToRefreshSlidingContainer
import com.tangem.core.ui.components.haze.hazeEffectTangem
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.components.rememberIsKeyboardVisible
import com.tangem.core.ui.components.sheetscaffold.*
import com.tangem.core.ui.ds.topbar.collapsing.TangemCollapsingAppBarBehavior
import com.tangem.core.ui.ds.topbar.collapsing.TangemCollapsingTopBar
import com.tangem.core.ui.ds.topbar.collapsing.rememberTangemExitUntilCollapsedScrollBehavior
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.*
import com.tangem.core.ui.utils.TangemSharedTransitionLayout
import com.tangem.feature.wallet.presentation.common.preview.WalletScreenPreviewData
import com.tangem.feature.wallet.presentation.wallet.state.model.NOT_INITIALIZED_WALLET_INDEX
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletBalanceUM
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletScreenState
import com.tangem.feature.wallet.presentation.wallet.ui.components.MarketsHint
import com.tangem.feature.wallet.presentation.wallet.ui.components.MarketsTooltip
import com.tangem.feature.wallet.presentation.wallet.ui.components.common.WalletBalance
import com.tangem.feature.wallet.presentation.wallet.ui.components.common.WalletListContent
import com.tangem.feature.wallet.presentation.wallet.ui.components.common.WalletPagerIndicator
import com.tangem.feature.wallet.presentation.wallet.ui.components.common.WalletTopBar
import com.tangem.feature.wallet.presentation.wallet.ui.utils.lazyListStateMapSaver
import com.tangem.features.tangempay.component.TangemPayMainBlockComponent
import com.tangem.features.tangempay.entity.TangemPayMainUM
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeTint
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val MARKET_HINT_THRESHOLD = 0.5f

@OptIn(ExperimentalDecomposeApi::class)
@Composable
internal fun WalletScreen2(
    state: WalletScreenState,
    tangemPayComponent: TangemPayMainBlockComponent,
    modifier: Modifier = Modifier,
    bottomSheetContent: @Composable (() -> Unit),
    bottomSheetHeaderHeightProvider: () -> Dp,
    onBottomSheetStateChange: (BottomSheetState) -> Unit,
) {
    // It means that screen is still initializing
    if (state.selectedWalletIndex == NOT_INITIALIZED_WALLET_INDEX) return

    val statusBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getTop(this).toDp() }

    val walletsPagerState = rememberPagerState(
        initialPage = state.selectedWalletIndex,
        pageCount = { state.wallets2.size },
    )

    val listStates = rememberSaveable(saver = lazyListStateMapSaver(walletsPagerState.pageCount)) {
        mutableMapOf<Int, LazyListState>().apply {
            repeat(walletsPagerState.pageCount) { index -> put(index, LazyListState()) }
        }
    }

    val isTopOverscrollEnabled by remember {
        derivedStateOf {
            val listState = listStates[walletsPagerState.currentPage] ?: return@derivedStateOf false
            listState.layoutInfo.totalItemsCount > 0 &&
                !listState.canScrollBackward && !listState.canScrollForward ||
                listState.canScrollBackward && !listState.canScrollForward
        }
    }

    val partialCollapsedHeight = 64.dp + statusBarHeight
    val balanceBlockHeight = 320.dp + partialCollapsedHeight
    val behavior = rememberTangemExitUntilCollapsedScrollBehavior(
        expandedHeight = balanceBlockHeight,
        partialCollapsedHeight = partialCollapsedHeight,
        snapAnimationSpec = spring(stiffness = Spring.StiffnessMedium),
        isTopOverscrollEnabled = isTopOverscrollEnabled,
    )

    val coroutineScope = rememberCoroutineScope()

    WalletContent2(
        state = state,
        walletsPagerState = walletsPagerState,
        tangemPayComponent = tangemPayComponent,
        behavior = behavior,
        bottomSheetContent = bottomSheetContent,
        bottomSheetHeaderHeightProvider = bottomSheetHeaderHeightProvider,
        onBottomSheetStateChange = onBottomSheetStateChange,
        modifier = modifier,
        listStates = listStates,
    )

    WalletEventEffect(
        walletsPagerState = walletsPagerState,
        event = state.event,
        onCollapseBalance = {
            if (behavior.state.collapsedFraction < 1f) {
                coroutineScope.launch {
                    behavior.state.collapse()
                }
            }
        },
    )
}

@Suppress("LongMethod", "LongParameterList", "CyclomaticComplexMethod")
@Composable
private fun WalletContent2(
    state: WalletScreenState,
    walletsPagerState: PagerState,
    tangemPayComponent: TangemPayMainBlockComponent,
    behavior: TangemCollapsingAppBarBehavior,
    listStates: Map<Int, LazyListState>,
    modifier: Modifier = Modifier,
    bottomSheetHeaderHeightProvider: () -> Dp,
    onBottomSheetStateChange: (BottomSheetState) -> Unit,
    bottomSheetContent: @Composable (() -> Unit),
) {
    val density = LocalDensity.current
    val bottomBarHeight = with(density) { WindowInsets.systemBars.getBottom(this).toDp() }

    var walletBalance by remember { mutableStateOf<TextReference?>(TextReference.EMPTY) }
    var pullToRefreshConfig by remember {
        mutableStateOf(
            state.wallets2.getOrNull(state.selectedWalletIndex)?.pullToRefreshConfig,
        )
    }

    BaseScaffoldWithMarkets(
        modifier = modifier,
        state = state,
        bottomSheetHeaderHeightProvider = bottomSheetHeaderHeightProvider,
        onBottomSheetStateChange = onBottomSheetStateChange,
        bottomSheetContent = bottomSheetContent,
        appBarContent = {
            WalletTopBar(
                topBarConfig = state.topBarConfig,
                walletBalance = walletBalance,
                behavior = behavior,
            )
        },
    ) { paddingValues, bottomSheetState ->
        val marketHintApproxHeight = 140.dp

        val contentPadding = PaddingValues(
            bottom = paddingValues.calculateBottomPadding() + marketHintApproxHeight,
        )

        LaunchedEffect(walletsPagerState.currentPage) {
            if (walletsPagerState.currentPage != state.selectedWalletIndex) {
                state.onWalletChange(walletsPagerState.currentPage, false)
            }
        }

        val canPagerScroll by remember { derivedStateOf { behavior.state.heightOffset == 0f } }

        val pullToRefreshState = rememberPullToRefreshState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSourceTangem(zIndex = -2f),
        ) {
            NorthernLightsBackground(
                containerColor = if (LocalIsInDarkTheme.current) {
                    TangemTheme.colors2.surface.level1
                } else {
                    TangemTheme.colors2.surface.level2
                },
                modifier = Modifier
                    .graphicsLayer { alpha = 1 - behavior.state.collapsedFraction * 2 }
                    .matchParentSize(),
            )

            WalletPagerIndicator(
                pagerState = walletsPagerState,
                pullToRefreshState = pullToRefreshState,
                pullToRefreshConfig = pullToRefreshConfig,
                behavior = behavior,
            )

            val overlay = TangemTheme.colors2.overlay.overlayPrimary

            HorizontalPager(
                state = walletsPagerState,
                userScrollEnabled = canPagerScroll,
                beyondViewportPageCount = 1,
                modifier = Modifier.hazeEffectTangem {
                    fallbackTint = HazeTint(color = overlay)
                    progressive = HazeProgressive.verticalGradient(
                        startIntensity = 1f,
                        endIntensity = 1f,
                        preferPerformance = true,
                    )
                },
            ) { currentWalletIndex ->
                val listState = listStates[currentWalletIndex] ?: rememberLazyListState()

                val currentWallet = state.wallets2.getOrElse(currentWalletIndex) {
                    state.wallets2[state.selectedWalletIndex]
                }

                LaunchedEffect(walletsPagerState.currentPage, currentWallet.walletsBalanceUM) {
                    if (walletsPagerState.currentPage == currentWalletIndex) {
                        walletBalance =
                            (currentWallet.walletsBalanceUM as? WalletBalanceUM.Content)?.balanceInAppBar
                    }
                }
                LaunchedEffect(walletsPagerState.currentPage, currentWallet.pullToRefreshConfig) {
                    if (walletsPagerState.currentPage == currentWalletIndex) {
                        pullToRefreshConfig = currentWallet.pullToRefreshConfig
                    }
                }

                val isShowMarketsHint by remember {
                    derivedStateOf {
                        behavior.state.collapsedFraction > MARKET_HINT_THRESHOLD &&
                            listState.layoutInfo.totalItemsCount > 0 &&
                            !listState.canScrollBackward && !listState.canScrollForward ||
                            listState.canScrollBackward && !listState.canScrollForward
                    }
                }

                val pageSlideAlpha by rememberPageAlpha(walletsPagerState, currentWalletIndex)

                TangemSharedTransitionLayout(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(pageSlideAlpha),
                ) {
                    TangemPullToRefreshSlidingContainer(
                        state = pullToRefreshState,
                        config = currentWallet.pullToRefreshConfig,
                        indicatorOffset = with(LocalDensity.current) {
                            behavior.state.partialHeightLimit.toDp()
                        },
                    ) {
                        TangemCollapsingTopBar(
                            state = behavior.state,
                            collapsingPart = {
                                WalletBalance(
                                    behavior = behavior,
                                    walletBalanceUM = currentWallet.walletsBalanceUM,
                                    buttons = currentWallet.buttons,
                                    isBalanceHidden = state.isHidingMode,
                                )
                            },
                            body = {
                                WalletListContent(
                                    currentWallet = currentWallet,
                                    listState = listState,
                                    isBalanceHidden = state.isHidingMode,
                                    contentPadding = contentPadding,
                                    tangemPayComponent = tangemPayComponent,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .nestedScroll(behavior.nestedScrollConnection),
                                )
                            },
                        )
                    }

                    val peekHeight =
                        bottomSheetHeaderHeightProvider() + handComposableComponentHeight + bottomBarHeight
                    MarketsHint(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = peekHeight + TangemTheme.dimens2.x7),
                        isVisible = isShowMarketsHint,
                    )
                }
            }

            MarketsTooltip(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth(),
                isVisible = state.showMarketsOnboarding,
                availableHeight = LocalWindowSize.current.height,
                bottomSheetState = bottomSheetState,
                onCloseClick = state.onDismissMarketsTooltip,
            )
        }
    }
}

@Suppress("LongParameterList", "LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private inline fun BaseScaffoldWithMarkets(
    state: WalletScreenState,
    bottomSheetHeaderHeightProvider: () -> Dp,
    modifier: Modifier = Modifier,
    noinline onBottomSheetStateChange: (BottomSheetState) -> Unit,
    crossinline appBarContent: @Composable () -> Unit,
    crossinline bottomSheetContent: @Composable () -> Unit,
    crossinline content: @Composable (PaddingValues, TangemSheetState) -> Unit,
) {
    val density = LocalDensity.current
    val bottomBarHeight = with(density) { WindowInsets.systemBars.getBottom(density = this).toDp() }
    val peekHeight = bottomSheetHeaderHeightProvider() + TangemTheme.dimens2.x3 + bottomBarHeight

    val coroutineScope = rememberCoroutineScope()
    val background = TangemTheme.colors2.surface.level2

    val bottomSheetState = rememberTangemStandardBottomSheetState()
    val scaffoldState = rememberTangemBottomSheetScaffoldState(bottomSheetState = bottomSheetState)

    CompositionLocalProvider(
        LocalMainBottomSheetColor provides remember(background) { mutableStateOf(background) },
    ) {
        val backgroundColor by LocalMainBottomSheetColor.current
        var isSearchFieldFocused by remember { mutableStateOf(false) }
        val isNavBarVisible = remember { mutableStateOf(true) }

        BottomSheetStateEffects(
            bottomSheetState = bottomSheetState,
            onBottomSheetStateChange = onBottomSheetStateChange,
            navigationBarVisible = isNavBarVisible,
            isSearchFieldFocused = isSearchFieldFocused,
        )

        Box(modifier = modifier) {
            TangemBottomSheetScaffold(
                containerColor = Color.Unspecified,
                scaffoldState = scaffoldState,
                sheetPeekHeight = peekHeight,
                bottomSheet = {
                    BottomSheet(
                        bottomSheetState = bottomSheetState,
                        backgroundColor = backgroundColor,
                        peekHeight = peekHeight,
                        onFocusChange = { focusState ->
                            isSearchFieldFocused = focusState.isFocused
                        },
                    ) {
                        bottomSheetContent()
                    }
                },
                content = { paddingValues ->
                    content(paddingValues, bottomSheetState)
                    appBarContent()

                    BottomSheetScrim(
                        color = Color.Black.copy(alpha = .40f),
                        visible = bottomSheetState.targetValue == TangemSheetValue.Expanded,
                        onDismissRequest = {
                            coroutineScope.launch { bottomSheetState.partialExpand() }
                        },
                    )
                },
            )

            AnimatedVisibility(
                modifier = Modifier.align(Alignment.BottomCenter),
                visible = isNavBarVisible.value,
            ) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .background(backgroundColor)
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

@Composable
private fun BottomSheet(
    bottomSheetState: TangemSheetState,
    backgroundColor: Color,
    peekHeight: Dp,
    onFocusChange: (FocusState) -> Unit,
    bottomSheetContent: @Composable () -> Unit,
) {
    val isKeyboardVisible by rememberIsKeyboardVisible()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val statusBarHeight = with(density) { WindowInsets.statusBars.getTop(density = this).toDp() }

    val maxHeight = LocalWindowSize.current.height
    val shape = RoundedCornerShape(
        topStart = TangemTheme.dimens2.x8,
        topEnd = TangemTheme.dimens2.x8,
    )
    CustomBottomSheet(
        state = bottomSheetState,
        peekHeight = peekHeight,
        content = {
            // hide bottom sheet when back pressed
            BackHandler(
                isKeyboardVisible.not() &&
                    bottomSheetState.currentValue == TangemSheetValue.Expanded,
            ) {
                coroutineScope.launch { bottomSheetState.partialExpand() }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
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
                Box(modifier = Modifier.fillMaxWidth()) {
                    BottomFade(
                        gradientBrush = Brush.verticalGradient(
                            colors = listOf(
                                TangemTheme.colors2.shadow.min,
                                TangemTheme.colors2.shadow.max,
                            ),
                        ),
                        modifier = Modifier
                            .offset(y = TangemTheme.dimens2.x5.unaryMinus()),
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        TangemBottomSheetDraggableHeader()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shape)
                                .background(backgroundColor)
                                .onFocusChanged(onFocusChange),
                        ) {
                            bottomSheetContent()
                        }
                    }
                }
            }
        },
    )
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
private fun rememberPageAlpha(pagerState: PagerState, currentPageIndex: Int): State<Float> {
    return remember {
        derivedStateOf {
            val pageOffset = pagerState.currentPageOffsetFraction
            val currentPage = pagerState.currentPage

            when {
                // Current page is being swiped away
                currentPageIndex == currentPage -> {
                    1f - abs(pageOffset) * 2f
                }
                // Target page is being swiped in
                currentPageIndex == pagerState.targetPage -> {
                    (abs(pageOffset) * 2f - 1f).coerceAtLeast(0f)
                }
                // Other pages remain invisible
                else -> 0f
            }.coerceIn(0f, 1f)
        }
    }
}

// region Preview
@OptIn(ExperimentalDecomposeApi::class)
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WalletScreen2_Preview(@PreviewParameter(WalletScreen2PreviewProvider::class) data: WalletScreenState) {
    TangemThemePreviewRedesign {
        WalletScreen2(
            state = data,
            tangemPayComponent = object : TangemPayMainBlockComponent {
                override fun LazyListScope.tangemPayMainContent(
                    state: TangemPayMainUM,
                    isBalanceHidden: Boolean,
                    modifier: Modifier,
                ) {
                }
            },
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
            WalletScreenPreviewData.defaultState,
            WalletScreenPreviewData.emptyState,
            WalletScreenPreviewData.defaultAccountState,
            WalletScreenPreviewData.emptyAccountState,
            WalletScreenPreviewData.lockedState,
        )
}
// endregion