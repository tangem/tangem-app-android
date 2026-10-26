package com.tangem.feature.wallet.presentation.wallet.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.tangem.core.ui.components.background.northernlights.NorthernLightsBackground
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.components.containers.pullToRefresh.TangemPullToRefreshSlidingContainer
import com.tangem.core.ui.components.containers.pullToRefresh.getPullToRefreshIndicatorOffset
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.components.rememberIsKeyboardVisible
import com.tangem.core.ui.ds.topbar.collapsing.TangemCollapsingAppBarBehavior
import com.tangem.core.ui.ds.topbar.collapsing.TangemCollapsingTopBar
import com.tangem.core.ui.ds.topbar.collapsing.rememberTangemExitUntilCollapsedScrollBehavior
import com.tangem.core.ui.ds2.shtorka.ShtorkaExpansion
import com.tangem.core.ui.ds2.shtorka.TangemShtorka
import com.tangem.core.ui.ds2.shtorka.TangemShtorkaState
import com.tangem.core.ui.ds2.shtorka.rememberTangemShtorkaState
import com.tangem.core.ui.extensions.TextReference
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
import com.tangem.features.collectibles.api.walletblock.CollectiblesWalletBlockComponent
import com.tangem.features.collectibles.api.walletblock.CollectiblesWalletBlockUM
import com.tangem.features.polymarket.api.walletblock.PolymarketWalletBlockComponent
import com.tangem.features.polymarket.api.walletblock.PolymarketWalletBlockUM
import com.tangem.features.tangempay.component.TangemPayMainBlockComponent
import com.tangem.features.tangempay.entity.TangemPayMainUM
import com.tangem.features.moneyaccount.main.component.MoneyAccountMainBlockComponent
import com.tangem.features.moneyaccount.main.entity.MoneyAccountMainUM
import com.tangem.utils.annotations.RemoveWithToggle
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalDecomposeApi::class)
@Suppress("LongParameterList")
@Composable
internal fun WalletScreen(
    state: WalletScreenState,
    tangemPayComponent: TangemPayMainBlockComponent,
    moneyAccountComponent: MoneyAccountMainBlockComponent,
    polymarketComponent: PolymarketWalletBlockComponent,
    collectiblesComponent: CollectiblesWalletBlockComponent,
    jointAccountComponent: JointAccountMainBlockComponent,
    modifier: Modifier = Modifier,
    @RemoveWithToggle("TWI_1841_HIDE_MARKETING_BANNERS_WHILE_SYSTEM_ENABLED")
    isHideMarketingWhileSystemEnabled: Boolean = false,
    isShtorkaBlurEnabled: () -> Boolean = { true },
    isShtorkaPartialExpansionSupported: Boolean = false,
    promoBannersBlockComponent: PromoBannersBlockComponent? = null,
    shtorkaHeaderContent: @Composable (sheetExpansion: State<ShtorkaExpansion>, onExpandSheet: () -> Unit) -> Unit,
    shtorkaContent: @Composable (onExpandSheet: () -> Unit) -> Unit,
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
        moneyAccountComponent = moneyAccountComponent,
        polymarketComponent = polymarketComponent,
        collectiblesComponent = collectiblesComponent,
        jointAccountComponent = jointAccountComponent,
        behavior = behavior,
        isHideMarketingWhileSystemEnabled = isHideMarketingWhileSystemEnabled,
        isShtorkaBlurEnabled = isShtorkaBlurEnabled,
        isShtorkaPartialExpansionSupported = isShtorkaPartialExpansionSupported,
        shtorkaHeaderContent = shtorkaHeaderContent,
        shtorkaContent = shtorkaContent,
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
    moneyAccountComponent: MoneyAccountMainBlockComponent,
    polymarketComponent: PolymarketWalletBlockComponent,
    collectiblesComponent: CollectiblesWalletBlockComponent,
    jointAccountComponent: JointAccountMainBlockComponent,
    behavior: TangemCollapsingAppBarBehavior,
    listStates: ImmutableMap<Int, LazyListState>,
    modifier: Modifier = Modifier,
    @RemoveWithToggle("TWI_1841_HIDE_MARKETING_BANNERS_WHILE_SYSTEM_ENABLED")
    isHideMarketingWhileSystemEnabled: Boolean = false,
    isShtorkaBlurEnabled: () -> Boolean = { true },
    isShtorkaPartialExpansionSupported: Boolean = false,
    promoBannersBlockComponent: PromoBannersBlockComponent? = null,
    onBottomSheetStateChange: (BottomSheetState) -> Unit,
    shtorkaHeaderContent: @Composable (sheetExpansion: State<ShtorkaExpansion>, onExpandSheet: () -> Unit) -> Unit,
    shtorkaContent: @Composable (onExpandSheet: () -> Unit) -> Unit,
) {
    var walletBalance by remember { mutableStateOf<TextReference?>(TextReference.EMPTY) }
    var pullToRefreshConfig by remember {
        mutableStateOf(
            state.wallets2.getOrNull(state.selectedWalletIndex)?.pullToRefreshConfig,
        )
    }
    var subtitleBottom by remember { mutableStateOf(0.dp) }

    ShtorkaScaffoldWithMarkets(
        modifier = modifier,
        state = state,
        isBlurEnabled = isShtorkaBlurEnabled,
        isPartialExpansionSupported = isShtorkaPartialExpansionSupported,
        shtorkaHeaderContent = shtorkaHeaderContent,
        onBottomSheetStateChange = onBottomSheetStateChange,
        bottomSheetContent = shtorkaContent,
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
                8.dp,
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

                    // A collapsedFraction this small means the user is at the very top of the list
                    @Suppress("MagicNumber")
                    LaunchedEffect(listState) {
                        snapshotFlow { listState.layoutInfo.totalItemsCount }
                            .collectLatest {
                                if (behavior.state.collapsedFraction < 0.01f &&
                                    listState.firstVisibleItemIndex != 0
                                ) {
                                    listState.scrollToItem(index = 0)
                                }
                            }
                    }

                    @Suppress("MagicNumber")
                    val isShowMarketsHint by remember {
                        derivedStateOf {
                            behavior.state.collapsedFraction > 0.5f &&
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
                                        promoBannersBlockComponent = promoBannersBlockComponent?.takeIf {
                                            !isHideMarketingWhileSystemEnabled || currentWallet.isMarketingBlockVisible
                                        },
                                        walletId = currentWalletId,
                                        moneyAccountComponent = moneyAccountComponent,
                                        polymarketComponent = polymarketComponent,
                                        collectiblesComponent = collectiblesComponent,
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
                isVisible = state.shouldShowMarketsOnboarding,
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

private val SkippedWithoutPartialExpansion = setOf(ShtorkaSemiOpenDetent)

@Suppress("LongParameterList", "LongMethod")
@Composable
private fun ShtorkaScaffoldWithMarkets(
    state: WalletScreenState,
    isPartialExpansionSupported: Boolean,
    onBottomSheetStateChange: (BottomSheetState) -> Unit,
    modifier: Modifier = Modifier,
    isBlurEnabled: () -> Boolean = { true },
    shtorkaHeaderContent: @Composable (sheetExpansion: State<ShtorkaExpansion>, onExpandSheet: () -> Unit) -> Unit,
    appBarContent: @Composable () -> Unit,
    bottomSheetContent: @Composable (onExpandSheet: () -> Unit) -> Unit,
    content: @Composable (PaddingValues, WalletSheetHandle) -> Unit,
) {
    val density = LocalDensity.current
    val bottomBarHeight = with(density) { WindowInsets.systemBars.getBottom(density = this).toDp() }

    val shtorkaState = rememberTangemShtorkaState(
        detents = ShtorkaDetents,
        initialDetent = ShtorkaCollapsedDetent,
        skippedDetents = if (isPartialExpansionSupported) emptySet() else SkippedWithoutPartialExpansion,
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
            isKeyboardVisible = isKeyboardVisible,
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
        val sheetExpansion = remember(shtorkaState) {
            derivedStateOf {
                when (shtorkaState.targetDetent) {
                    ShtorkaCollapsedDetent -> ShtorkaExpansion.PILL
                    TangemShtorka.Detent.Full -> ShtorkaExpansion.FULL
                    else -> ShtorkaExpansion.PARTIAL
                }
            }
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
                isMaterial = isBlurEnabled(),
                color = backgroundColor,
                // hasFocus, not isFocused: the focused node is the search field somewhere below,
                // this modifier only sees it as a descendant
                modifier = Modifier.onFocusChanged { focusState ->
                    isSearchFieldFocused = focusState.hasFocus
                },
                // The header draws its own small drag tip with the search bar centered around it
                showDragHandle = false,
                header = {
                    ShtorkaSheetHeader(expansion = sheetExpansion) {
                        shtorkaHeaderContent(sheetExpansion, onExpandSheet)
                    }
                },
            ) {
                // The content offsets itself by ShtorkaSheetHeaderHeight (passed by the caller),
                // so it starts below the pinned grabber + search bar.
                bottomSheetContent(onExpandSheet)
            }
        }

        LaunchedEffect(state.shouldShowMarketsOnboarding, isExpanded) {
            if (state.shouldShowMarketsOnboarding && isExpanded) {
                state.onDismissMarketsTooltip()
            }
        }
    }
}

@Composable
private fun ShtorkaStateEffects(
    shtorkaState: TangemShtorkaState,
    isExpanded: Boolean,
    isKeyboardVisible: Boolean,
    isSearchFieldFocused: Boolean,
    onBottomSheetStateChange: (BottomSheetState) -> Unit,
) {
    // raise the shtorka whenever the search field is being typed into — keyed on the focus too, so
    // focusing the field while the keyboard is already up (it was raised by another field) counts
    LaunchedEffect(isKeyboardVisible, isSearchFieldFocused) {
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
            moneyAccountComponent = object : MoneyAccountMainBlockComponent {
                override fun LazyListScope.moneyAccountMainContent(
                    state: MoneyAccountMainUM,
                    isBalanceHidden: Boolean,
                    modifier: Modifier,
                ) {
                }
            },
            polymarketComponent = object : PolymarketWalletBlockComponent {
                override fun LazyListScope.polymarketWalletBlockContent(
                    state: PolymarketWalletBlockUM,
                    isBalanceHidden: Boolean,
                    modifier: Modifier,
                ) {
                }
            },
            collectiblesComponent = object : CollectiblesWalletBlockComponent {
                override fun LazyListScope.collectiblesWalletBlockContent(
                    state: CollectiblesWalletBlockUM,
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
            shtorkaHeaderContent = { _, _ -> },
            shtorkaContent = {},
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