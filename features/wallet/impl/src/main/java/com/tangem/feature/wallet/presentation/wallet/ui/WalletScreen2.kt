package com.tangem.feature.wallet.presentation.wallet.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.tangem.core.ui.components.atoms.Hand
import com.tangem.core.ui.components.atoms.handComposableComponentHeight
import com.tangem.core.ui.components.background.northernlights.NorthernLightsBackground
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.components.haze.hazeEffectTangem
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.components.rememberIsKeyboardVisible
import com.tangem.core.ui.components.sheetscaffold.*
import com.tangem.core.ui.components.snackbar.CopiedTextSnackbar
import com.tangem.core.ui.components.snackbar.TangemSnackbar
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.event.StateEvent
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.*
import com.tangem.core.ui.test.MainScreenTestTags
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.preview.WalletScreenPreviewData.accountScreenState
import com.tangem.feature.wallet.presentation.common.preview.WalletScreenPreviewData.accountScreenWithEmptyTokensState
import com.tangem.feature.wallet.presentation.common.preview.WalletScreenPreviewData.walletScreenState
import com.tangem.feature.wallet.presentation.wallet.state.model.NOT_INITIALIZED_WALLET_INDEX
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletEvent
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletScreenState
import dev.chrisbanes.haze.HazeProgressive
import kotlinx.coroutines.launch

@OptIn(ExperimentalDecomposeApi::class)
@Composable
internal fun WalletScreen2(
    state: WalletScreenState,
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalDecomposeApi::class)
@Suppress("LongMethod", "LongParameterList", "UnusedPrivateMember")
@Composable
private fun WalletContent2(
    state: WalletScreenState,
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
    // val selectedWalletIndex by remember(state.selectedWalletIndex) { mutableIntStateOf(state.selectedWalletIndex) }
    // val selectedWallet = state.wallets2.getOrElse(selectedWalletIndex) { state.wallets2[state.selectedWalletIndex] }

    val statusBarHeight = with(LocalDensity.current) { WindowInsets.statusBars.getBottom(this).toDp() }

    val listState = rememberLazyListState()

    val partialCollapsedHeight = 64.dp + statusBarHeight

    val scaffoldContent: @Composable (PaddingValues?) -> Unit = { _ ->
        Box(Modifier.fillMaxSize()) {
            NorthernLightsBackground(Modifier.matchParentSize())
        }

        val pagerState = rememberPagerState(
            initialPage = state.selectedWalletIndex,
            pageCount = { state.wallets2.size },
        )

        LaunchedEffect(pagerState.currentPage) {
            if (pagerState.currentPage != state.selectedWalletIndex) {
                state.onWalletChange(pagerState.currentPage, false)
            }
        }
    }

    BaseScaffoldWithMarkets(
        state = state,
        listState = listState,
        snackbarHostState = snackbarHostState,
        bottomSheetHeaderHeightProvider = bottomSheetHeaderHeightProvider,
        onBottomSheetStateChange = onBottomSheetStateChange,
        bottomSheetContent = bottomSheetContent,
        content = scaffoldContent,
    )
}

@Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod", "UnusedPrivateMember")
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
    val background = TangemTheme.colors2.surface.level2

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

        Box(modifier = modifier) {
            TangemBottomSheetScaffold(
                modifier = Modifier.background(
                    brush = Brush.verticalGradient(
                        listOf(
                            TangemTheme.colors2.surface.level1,
                            TangemTheme.colors2.surface.level2,
                        ),
                    ),
                ),
                snackbarHost = { snackbarHostState ->
                    WalletSnackbarHost(
                        snackbarHostState = snackbarHostState,
                        event = state.event,
                        modifier = Modifier
                            .padding(bottom = TangemTheme.dimens.spacing4)
                            .navigationBarsPadding(),
                    )
                },
                containerColor = Color.Unspecified,
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
@OptIn(ExperimentalDecomposeApi::class)
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WalletScreen2_Preview(@PreviewParameter(WalletScreen2PreviewProvider::class) data: WalletScreenState) {
    TangemThemePreviewRedesign {
        WalletScreen2(
            state = data,
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