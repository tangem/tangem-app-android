package com.tangem.feature.wallet.presentation.wallet.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.tangem.core.ui.components.background.northernlights.NorthernLightsBackground
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheetDraggableHeader
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.components.containers.pullToRefresh.TangemPullToRefreshSlidingContainer
import com.tangem.core.ui.components.containers.pullToRefresh.getPullToRefreshIndicatorOffset
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.components.rememberIsKeyboardVisible
import com.tangem.core.ui.components.sheetscaffold.*
import com.tangem.core.ui.ds.topbar.collapsing.TangemCollapsingAppBarBehavior
import com.tangem.core.ui.ds.topbar.collapsing.TangemCollapsingTopBar
import com.tangem.core.ui.ds.topbar.collapsing.rememberTangemExitUntilCollapsedScrollBehavior
import com.tangem.core.ui.ds2.shtorka.TangemShtorka
import com.tangem.core.ui.ds2.shtorka.TangemShtorkaState
import com.tangem.core.ui.ds2.shtorka.rememberTangemShtorkaState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.softLayerShadow
import com.tangem.core.ui.res.*
import com.tangem.core.ui.utils.TangemSharedTransitionLayout
import com.tangem.feature.wallet.presentation.common.preview.WalletScreenPreviewData
import com.tangem.feature.wallet.presentation.wallet.state.model.NOT_INITIALIZED_WALLET_INDEX
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletBalanceUM
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletScreenState
import com.tangem.feature.wallet.presentation.wallet.ui.components.MarketsHint
import com.tangem.feature.wallet.presentation.wallet.ui.components.MarketsTooltip
import com.tangem.feature.wallet.presentation.wallet.ui.components.ShtorkaSheetHeader
import com.tangem.feature.wallet.presentation.wallet.ui.components.ShtorkaSheetHeaderHeight
import com.tangem.feature.wallet.presentation.wallet.ui.components.common.WalletBalance
import com.tangem.feature.wallet.presentation.wallet.ui.components.common.WalletListContent
import com.tangem.feature.wallet.presentation.wallet.ui.components.common.WalletPagerIndicator
import com.tangem.feature.wallet.presentation.wallet.ui.components.common.WalletTopBar
import com.tangem.feature.wallet.presentation.wallet.ui.utils.lazyListStateMapSaver
import com.tangem.features.jointaccount.main.JointAccountMainBlockComponent
import com.tangem.features.jointaccount.main.JointAccountMainUM
import com.tangem.features.feed.v2.FeedV2Component
import com.tangem.features.promobanners.api.PromoBannersBlockComponent
import com.tangem.features.tangempay.component.TangemPayMainBlockComponent
import com.tangem.features.tangempay.entity.TangemPayMainUM
import com.tangem.features.virtualaccount.main.component.VirtualAccountMainBlockComponent
import com.tangem.features.virtualaccount.main.entity.VirtualAccountMainUM
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val MARKET_HINT_THRESHOLD = 0.5f

// collapsedFraction below this is treated as "fully expanded" (user is at the very top of the list)
private const val FULLY_EXPANDED_THRESHOLD = 0.01f

@OptIn(ExperimentalDecomposeApi::class)
@Suppress("LongParameterList")
@Composable
internal fun WalletScreen(
    state: WalletScreenState,
    tangemPayComponent: TangemPayMainBlockComponent,
    virtualAccountComponent: VirtualAccountMainBlockComponent,
    jointAccountComponent: JointAccountMainBlockComponent,
    modifier: Modifier = Modifier,
    isNewShtorkaEnabled: Boolean = false,
    promoBannersBlockComponent: PromoBannersBlockComponent? = null,
    shtorkaHeaderContent: @Composable (onExpandSheet: () -> Unit) -> Unit,
    bottomSheetContent: @Composable (onExpandSheet: () -> Unit) -> Unit,
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

    WalletContent(
        state = state,
        walletsPagerState = walletsPagerState,
        tangemPayComponent = tangemPayComponent,
        promoBannersBlockComponent = promoBannersBlockComponent,
        virtualAccountComponent = virtualAccountComponent,
        jointAccountComponent = jointAccountComponent,
        behavior = behavior,
        isNewShtorkaEnabled = isNewShtorkaEnabled,
        shtorkaHeaderContent = shtorkaHeaderContent,
        bottomSheetContent = bottomSheetContent,
        bottomSheetHeaderHeightProvider = bottomSheetHeaderHeightProvider,
        onBottomSheetStateChange = onBottomSheetStateChange,
        modifier = modifier,
        listStates = remember(listStates) { listStates.toImmutableMap() },
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
private fun WalletContent(
    state: WalletScreenState,
    walletsPagerState: PagerState,
    tangemPayComponent: TangemPayMainBlockComponent,
    virtualAccountComponent: VirtualAccountMainBlockComponent,
    jointAccountComponent: JointAccountMainBlockComponent,
    behavior: TangemCollapsingAppBarBehavior,
    listStates: ImmutableMap<Int, LazyListState>,
    modifier: Modifier = Modifier,
    isNewShtorkaEnabled: Boolean = false,
    promoBannersBlockComponent: PromoBannersBlockComponent? = null,
    bottomSheetHeaderHeightProvider: () -> Dp,
    onBottomSheetStateChange: (BottomSheetState) -> Unit,
    shtorkaHeaderContent: @Composable (onExpandSheet: () -> Unit) -> Unit,
    bottomSheetContent: @Composable (onExpandSheet: () -> Unit) -> Unit,
) {
    var walletBalance by remember { mutableStateOf<TextReference?>(TextReference.EMPTY) }
    var pullToRefreshConfig by remember {
        mutableStateOf(
            state.wallets2.getOrNull(state.selectedWalletIndex)?.pullToRefreshConfig,
        )
    }
    var subtitleBottom by remember { mutableStateOf(0.dp) }

    BaseScaffoldWithMarkets(
        modifier = modifier,
        state = state,
        isNewShtorkaEnabled = isNewShtorkaEnabled,
        shtorkaHeaderContent = shtorkaHeaderContent,
        bottomSheetHeaderHeightProvider = bottomSheetHeaderHeightProvider,
        onBottomSheetStateChange = onBottomSheetStateChange,
        bottomSheetContent = bottomSheetContent,
        appBarContent = {
            WalletTopBar(
                topBarConfig = state.topBarConfig,
                walletBalance = walletBalance,
                isBalanceHidden = state.isHidingMode,
                behavior = behavior,
            )
        },
    ) { paddingValues, sheet ->
        val density = LocalDensity.current
        var marketsHintHeight by remember { mutableStateOf(0.dp) }
        val collapsedBodyOverhang = with(density) { behavior.state.partialHeightLimit.toDp() }

        val contentPadding = PaddingValues(
            bottom = paddingValues.calculateBottomPadding() +
                marketsHintHeight +
                collapsedBodyOverhang +
                TangemTheme.dimens2.x2,
        )

        val selectedWalletIndex by rememberUpdatedState(state.selectedWalletIndex)
        LaunchedEffect(walletsPagerState) {
            // Only react to genuine settles and skip the page the pager was (re)created with, so a
            // programmatic scroll or pager recreation can't revert the selection to a stale page.
            snapshotFlow { walletsPagerState.settledPage }
                .drop(count = 1)
                .collectLatest { settledPage ->
                    if (settledPage != selectedWalletIndex) {
                        state.onWalletChange(settledPage, false)
                    }
                }
        }

        val canPagerScroll by remember { derivedStateOf { behavior.state.heightOffset == 0f } }

        val pullToRefreshState = rememberPullToRefreshState()
        val wallpaperHazeState = rememberHazeState()

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .hazeSourceTangem(zIndex = -2f),
        ) {
            val isSheetExpanded by remember(sheet) { derivedStateOf { sheet.isExpanded } }
            val organizeButtonBounds = remember { mutableStateMapOf<Int, Rect>() }
            CompositionLocalProvider(LocalHazeState provides wallpaperHazeState) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSourceTangem(zIndex = -1f),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(TangemTheme.colors3.bg.primary),
                    )
                    if (!isSheetExpanded) {
                        NorthernLightsBackground(
                            containerColor = TangemTheme.colors3.bg.primary,
                            modifier = Modifier
                                .graphicsLayer { alpha = 1 - behavior.state.collapsedFraction * 2 }
                                .fillMaxSize(),
                        )
                    }
                }

                WalletPagerIndicator(
                    pagerState = walletsPagerState,
                    pullToRefreshState = pullToRefreshState,
                    pullToRefreshConfig = pullToRefreshConfig,
                    behavior = behavior,
                    topOffset = subtitleBottom + 8.dp,
                )

                HorizontalPager(
                    state = walletsPagerState,
                    userScrollEnabled = canPagerScroll,
                    beyondViewportPageCount = 1,
                ) { currentWalletIndex ->
                    val listState = listStates[currentWalletIndex] ?: rememberLazyListState()

                    val currentWallet = state.wallets2.getOrElse(currentWalletIndex) {
                        state.wallets2[state.selectedWalletIndex]
                    }
                    val currentWalletId = currentWallet.walletsBalanceUM.id.stringValue

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

                    LaunchedEffect(listState) {
                        snapshotFlow { listState.layoutInfo.totalItemsCount }
                            .collectLatest {
                                if (behavior.state.collapsedFraction < FULLY_EXPANDED_THRESHOLD &&
                                    listState.firstVisibleItemIndex != 0
                                ) {
                                    listState.scrollToItem(index = 0)
                                }
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

                    val pullToRefreshContentOffset = getPullToRefreshIndicatorOffset(
                        pullToRefreshConfig = currentWallet.pullToRefreshConfig,
                        pullToRefreshState = pullToRefreshState,
                    )

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
                                    val balanceBlockHeight = with(LocalDensity.current) {
                                        -behavior.state.heightOffsetLimit.toDp()
                                    }
                                    WalletBalance(
                                        behavior = behavior,
                                        walletBalanceUM = currentWallet.walletsBalanceUM,
                                        buttons = currentWallet.buttons,
                                        isBalanceHidden = state.isHidingMode,
                                        modifier = Modifier.height(balanceBlockHeight),
                                        onSubtitleBottomChange = { newValue ->
                                            if (pullToRefreshContentOffset == 0.dp && newValue > subtitleBottom) {
                                                subtitleBottom = newValue
                                            }
                                        },
                                    )
                                },
                                body = {
                                    WalletListContent(
                                        currentWallet = currentWallet,
                                        listState = listState,
                                        isBalanceHidden = state.isHidingMode,
                                        contentPadding = contentPadding,
                                        tangemPayComponent = tangemPayComponent,
                                        promoBannersBlockComponent = promoBannersBlockComponent,
                                        walletId = currentWalletId,
                                        virtualAccountComponent = virtualAccountComponent,
                                        jointAccountComponent = jointAccountComponent,
                                        onOrganizeButtonBoundsChange = remember(currentWalletIndex) {
                                            { bounds ->
                                                if (bounds != null) {
                                                    organizeButtonBounds[currentWalletIndex] = bounds
                                                } else {
                                                    organizeButtonBounds.remove(currentWalletIndex)
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .nestedScroll(behavior.nestedScrollConnection),
                                    )
                                },
                            )
                        }
                        MarketsHint(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth(fraction = .6f)
                                .padding(bottom = paddingValues.calculateBottomPadding())
                                .onSizeChanged { size ->
                                    marketsHintHeight = with(density) { size.height.toDp() }
                                },
                            isVisible = isShowMarketsHint,
                            obstacleBounds = remember(currentWalletIndex) {
                                { organizeButtonBounds[currentWalletIndex] }
                            },
                        )
                    }
                }
            }

            MarketsTooltip(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth(),
                isVisible = state.showMarketsOnboarding,
                availableHeight = maxHeight,
                sheetTopInset = 12.dp,
                sheetTopOffset = remember(sheet) { sheet::topOffset },
                onCloseClick = state.onDismissMarketsTooltip,
                obstacleBounds = remember(walletsPagerState, organizeButtonBounds) {
                    { organizeButtonBounds[walletsPagerState.currentPage] }
                },
            )
        }
    }
}

/** Hosts the wallet content under the feed sheet — the legacy bottom sheet or the DS3 shtorka. */
@Suppress("LongParameterList")
@Composable
private fun BaseScaffoldWithMarkets(
    state: WalletScreenState,
    isNewShtorkaEnabled: Boolean,
    bottomSheetHeaderHeightProvider: () -> Dp,
    onBottomSheetStateChange: (BottomSheetState) -> Unit,
    modifier: Modifier = Modifier,
    shtorkaHeaderContent: @Composable (onExpandSheet: () -> Unit) -> Unit,
    appBarContent: @Composable () -> Unit,
    bottomSheetContent: @Composable (onExpandSheet: () -> Unit) -> Unit,
    content: @Composable (PaddingValues, WalletSheetHandle) -> Unit,
) {
    if (isNewShtorkaEnabled) {
        ShtorkaScaffoldWithMarkets(
            state = state,
            shtorkaHeaderContent = shtorkaHeaderContent,
            onBottomSheetStateChange = onBottomSheetStateChange,
            appBarContent = appBarContent,
            bottomSheetContent = bottomSheetContent,
            modifier = modifier,
            content = content,
        )
    } else {
        LegacySheetScaffoldWithMarkets(
            state = state,
            bottomSheetHeaderHeightProvider = bottomSheetHeaderHeightProvider,
            onBottomSheetStateChange = onBottomSheetStateChange,
            appBarContent = appBarContent,
            bottomSheetContent = bottomSheetContent,
            modifier = modifier,
            content = content,
        )
    }
}

@Suppress("LongParameterList", "LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private inline fun LegacySheetScaffoldWithMarkets(
    state: WalletScreenState,
    bottomSheetHeaderHeightProvider: () -> Dp,
    modifier: Modifier = Modifier,
    noinline onBottomSheetStateChange: (BottomSheetState) -> Unit,
    crossinline appBarContent: @Composable () -> Unit,
    crossinline bottomSheetContent: @Composable (onExpandSheet: () -> Unit) -> Unit,
    crossinline content: @Composable (PaddingValues, WalletSheetHandle) -> Unit,
) {
    val density = LocalDensity.current
    val bottomBarHeight = with(density) { WindowInsets.systemBars.getBottom(density = this).toDp() }
    val peekHeight = bottomSheetHeaderHeightProvider() + 12.dp + bottomBarHeight

    val coroutineScope = rememberCoroutineScope()

    val bottomSheetState = rememberTangemStandardBottomSheetState()
    val scaffoldState = rememberTangemBottomSheetScaffoldState(bottomSheetState = bottomSheetState)
    val sheetHandle = remember(bottomSheetState) { LegacyWalletSheetHandle(bottomSheetState) }

    val expandedBackground = TangemTheme.colors3.bg.primary
    val collapsedBackground = TangemTheme.colors3.bg.secondary
    val background by animateColorAsState(
        targetValue = if (bottomSheetState.targetValue == TangemSheetValue.Expanded) {
            expandedBackground
        } else {
            collapsedBackground
        },
        label = "bottomSheetBackground",
    )

    CompositionLocalProvider(
        LocalMainBottomSheetColor provides remember { mutableStateOf(background) }.apply { value = background },
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
                        bottomSheetContent {
                            coroutineScope.launch { bottomSheetState.expand() }
                        }
                    }
                },
                content = { paddingValues ->
                    content(paddingValues, sheetHandle)
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

/** Margin the collapsed shtorka card floats above the bottom inset (`ShtorkaCollapsedInset` in the DS). */
private val ShtorkaCollapsedInset = 24.dp

/** The collapsed shtorka shows exactly its sheet header — the grabber plus the search bar. */
private val ShtorkaCollapsedDetent = TangemShtorka.Detent.Height(value = ShtorkaSheetHeaderHeight)

/**
 * Semi-open detent ("Main + half"): sized to exactly the sheet header + top blocks + tab row,
 * so there is no free space under the tabs — tab content appears only at [TangemShtorka.Detent.Full].
 */
private val ShtorkaSemiOpenDetent = TangemShtorka.Detent.Height(
    value = ShtorkaSheetHeaderHeight + FeedV2Component.SemiOpenContentHeight,
)

private val ShtorkaDetents = listOf(ShtorkaCollapsedDetent, ShtorkaSemiOpenDetent, TangemShtorka.Detent.Full)

@Suppress("LongParameterList", "LongMethod")
@Composable
private fun ShtorkaScaffoldWithMarkets(
    state: WalletScreenState,
    onBottomSheetStateChange: (BottomSheetState) -> Unit,
    modifier: Modifier = Modifier,
    shtorkaHeaderContent: @Composable (onExpandSheet: () -> Unit) -> Unit,
    appBarContent: @Composable () -> Unit,
    bottomSheetContent: @Composable (onExpandSheet: () -> Unit) -> Unit,
    content: @Composable (PaddingValues, WalletSheetHandle) -> Unit,
) {
    val density = LocalDensity.current
    val bottomBarHeight = with(density) { WindowInsets.systemBars.getBottom(density = this).toDp() }

    val shtorkaState = rememberTangemShtorkaState(
        detents = ShtorkaDetents,
        initialDetent = ShtorkaCollapsedDetent,
    )
    val sheetHandle = remember(shtorkaState) { ShtorkaWalletSheetHandle(shtorkaState) }
    val isExpanded = sheetHandle.isExpanded

    val contentPadding = remember(bottomBarHeight) {
        PaddingValues(bottom = ShtorkaSheetHeaderHeight + ShtorkaCollapsedInset + bottomBarHeight)
    }

    val expandedBackground = TangemTheme.colors3.bg.primary
    val collapsedBackground = TangemTheme.colors3.bg.secondary
    val background by animateColorAsState(
        targetValue = if (isExpanded) expandedBackground else collapsedBackground,
        label = "shtorkaBackground",
    )

    CompositionLocalProvider(
        LocalMainBottomSheetColor provides remember { mutableStateOf(background) }.apply { value = background },
    ) {
        val backgroundColor by LocalMainBottomSheetColor.current
        var isSearchFieldFocused by remember { mutableStateOf(false) }
        val isKeyboardVisible by rememberIsKeyboardVisible()

        ShtorkaStateEffects(
            shtorkaState = shtorkaState,
            isExpanded = isExpanded,
            isSearchFieldFocused = isSearchFieldFocused,
            onBottomSheetStateChange = onBottomSheetStateChange,
        )

        // collapse the shtorka when back pressed at any raised detent
        val isRaised = shtorkaState.targetDetent != ShtorkaCollapsedDetent
        BackHandler(enabled = isRaised && !isKeyboardVisible) {
            shtorkaState.animateTo(ShtorkaCollapsedDetent)
        }

        val onExpandSheet = remember(shtorkaState) {
            { shtorkaState.animateTo(TangemShtorka.Detent.Full) }
        }

        Box(modifier = modifier.fillMaxSize()) {
            content(contentPadding, sheetHandle)
            appBarContent()

            BottomSheetScrim(
                color = Color.Black.copy(alpha = .40f),
                visible = isExpanded,
                onDismissRequest = { shtorkaState.animateTo(ShtorkaCollapsedDetent) },
            )

            TangemShtorka(
                state = shtorkaState,
                color = backgroundColor,
                modifier = Modifier.onFocusChanged { focusState ->
                    isSearchFieldFocused = focusState.isFocused
                },
                // The header draws its own small drag tip with the search bar centered around it
                showDragHandle = false,
                header = {
                    ShtorkaSheetHeader {
                        shtorkaHeaderContent(onExpandSheet)
                    }
                },
            ) {
                // The content offsets itself by ShtorkaSheetHeaderHeight (passed by the caller),
                // so it starts below the pinned grabber + search bar.
                bottomSheetContent(onExpandSheet)
            }
        }

        LaunchedEffect(state.showMarketsOnboarding, isExpanded) {
            if (state.showMarketsOnboarding && isExpanded) {
                state.onDismissMarketsTooltip()
            }
        }
    }
}

@Composable
private fun ShtorkaStateEffects(
    shtorkaState: TangemShtorkaState,
    isExpanded: Boolean,
    isSearchFieldFocused: Boolean,
    onBottomSheetStateChange: (BottomSheetState) -> Unit,
) {
    // expand the shtorka when the keyboard appears
    val isKeyboardVisible by rememberIsKeyboardVisible()
    LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible && isSearchFieldFocused) {
            shtorkaState.animateTo(TangemShtorka.Detent.Full)
        }
    }

    // hide the keyboard when the shtorka starts collapsing
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(shtorkaState) {
        snapshotFlow { shtorkaState.targetDetent is TangemShtorka.Detent.Full }
            .drop(count = 1)
            .collect { isFullDetent -> if (!isFullDetent) keyboardController?.hide() }
    }

    LaunchedEffect(isExpanded) {
        onBottomSheetStateChange(
            if (isExpanded) BottomSheetState.EXPANDED else BottomSheetState.COLLAPSED,
        )
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
        topStart = 32.dp,
        topEnd = 32.dp,
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
                    // expand bottom sheet when clicked on the drag handle
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        TangemBottomSheetDraggableHeader()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .softLayerShadow(
                                    radius = 8.dp,
                                    spread = 0.dp,
                                    color = Color.Black.copy(alpha = if (LocalIsInDarkTheme.current) .24f else .12f),
                                    shape = shape,
                                    offset = DpOffset(x = 0.dp, y = (-6).dp),
                                    isAlphaContentClip = true,
                                )
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
        WalletScreen(
            state = data,
            tangemPayComponent = object : TangemPayMainBlockComponent {
                override fun LazyListScope.tangemPayMainContent(
                    state: TangemPayMainUM,
                    isBalanceHidden: Boolean,
                    modifier: Modifier,
                ) {
                }
            },
            virtualAccountComponent = object : VirtualAccountMainBlockComponent {
                override fun LazyListScope.virtualAccountMainContent(
                    state: VirtualAccountMainUM,
                    isBalanceHidden: Boolean,
                    modifier: Modifier,
                ) {
                }
            },
            jointAccountComponent = object : JointAccountMainBlockComponent {
                override fun LazyListScope.jointAccountMainContent(
                    key: String,
                    state: JointAccountMainUM,
                    isBalanceHidden: Boolean,
                    modifier: Modifier,
                ) {
                }
            },
            shtorkaHeaderContent = {},
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