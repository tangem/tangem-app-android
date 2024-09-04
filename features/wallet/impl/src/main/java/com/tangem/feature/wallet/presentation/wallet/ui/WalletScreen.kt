package com.tangem.feature.wallet.presentation.wallet.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.tangem.core.ui.components.BottomFade
import com.tangem.core.ui.components.Keyboard
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.atoms.Hand
import com.tangem.core.ui.components.atoms.handComposableComponentHeight
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.chooseaddress.ChooseAddressBottomSheet
import com.tangem.core.ui.components.bottomsheets.chooseaddress.ChooseAddressBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.tokenreceive.TokenReceiveBottomSheet
import com.tangem.core.ui.components.bottomsheets.tokenreceive.TokenReceiveBottomSheetConfig
import com.tangem.core.ui.components.keyboardAsState
import com.tangem.core.ui.components.snackbar.CopiedTextSnackbar
import com.tangem.core.ui.components.snackbar.TangemSnackbar
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.event.StateEvent
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.core.ui.res.LocalWindowSize
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.TestTags
import com.tangem.core.ui.utils.WindowInsetsZero
import com.tangem.core.ui.utils.lineTo
import com.tangem.core.ui.utils.moveTo
import com.tangem.core.ui.utils.toPx
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.preview.WalletScreenPreviewData.walletScreenState
import com.tangem.feature.wallet.presentation.wallet.state.model.*
import com.tangem.feature.wallet.presentation.wallet.state.model.holder.TxHistoryStateHolder
import com.tangem.feature.wallet.presentation.wallet.ui.components.PushNotificationsBottomSheet
import com.tangem.feature.wallet.presentation.wallet.ui.components.TokenActionsBottomSheet
import com.tangem.feature.wallet.presentation.wallet.ui.components.WalletsList
import com.tangem.feature.wallet.presentation.wallet.ui.components.common.*
import com.tangem.feature.wallet.presentation.wallet.ui.components.multicurrency.organizeTokensButton
import com.tangem.feature.wallet.presentation.wallet.ui.components.singlecurrency.controlButtons
import com.tangem.feature.wallet.presentation.wallet.ui.components.singlecurrency.marketPriceBlock
import com.tangem.feature.wallet.presentation.wallet.ui.components.visa.BalancesAndLimitsBottomSheet
import com.tangem.feature.wallet.presentation.wallet.ui.components.visa.VisaTxDetailsBottomSheet
import com.tangem.feature.wallet.presentation.wallet.ui.components.visa.balancesAndLimitsBlock
import com.tangem.feature.wallet.presentation.wallet.ui.components.visa.depositButton
import com.tangem.feature.wallet.presentation.wallet.ui.utils.changeWalletAnimator
import com.tangem.features.markets.entry.BottomSheetState
import com.tangem.features.markets.entry.MarketsEntryComponent
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun WalletScreen(state: WalletScreenState, marketsEntryComponent: MarketsEntryComponent?) {
    BackHandler(onBack = state.onBackClick)

    // It means that screen is still initializing
    if (state.selectedWalletIndex == NOT_INITIALIZED_WALLET_INDEX) return

    val walletsListState = rememberLazyListState(initialFirstVisibleItemIndex = state.selectedWalletIndex)
    val snackbarHostState = remember(::SnackbarHostState)
    val isAutoScroll = remember { mutableStateOf(value = false) }

    var alertConfig by remember { mutableStateOf<WalletAlertState?>(value = null) }

    val config = alertConfig
    if (config != null) {
        WalletAlert(state = config, onDismiss = { alertConfig = null })
    }

    WalletContent(
        state = state,
        walletsListState = walletsListState,
        snackbarHostState = snackbarHostState,
        isAutoScroll = isAutoScroll,
        onAutoScrollReset = { isAutoScroll.value = false },
        marketsEntryComponent = marketsEntryComponent,
        alertConfig = alertConfig,
    )

    WalletEventEffect(
        event = state.event,
        selectedWalletIndex = state.selectedWalletIndex,
        walletsListState = walletsListState,
        snackbarHostState = snackbarHostState,
        onAlertConfigSet = { alertConfig = it },
        onAutoScrollSet = { isAutoScroll.value = true },
    )
}

@Suppress("LongMethod", "LongParameterList", "CyclomaticComplexMethod")
@Composable
private fun WalletContent(
    state: WalletScreenState,
    walletsListState: LazyListState,
    snackbarHostState: SnackbarHostState,
    isAutoScroll: State<Boolean>,
    marketsEntryComponent: MarketsEntryComponent?,
    alertConfig: WalletAlertState?,
    onAutoScrollReset: () -> Unit,
) {
    var selectedWalletIndex by remember(state.selectedWalletIndex) { mutableIntStateOf(state.selectedWalletIndex) }
    val selectedWallet = state.wallets.getOrElse(selectedWalletIndex) { state.wallets[state.selectedWalletIndex] }

    val scaffoldContent: @Composable () -> Unit = {
        val movableItemModifier = Modifier.changeWalletAnimator(walletsListState)

        val lazyTxHistoryItems = (selectedWallet as? TxHistoryStateHolder)?.let { walletState ->
            (walletState.txHistoryState as? TxHistoryState.Content)?.contentItems?.collectAsLazyPagingItems()
        }

        val txHistoryItems by remember(selectedWallet.walletCardState.id, lazyTxHistoryItems?.itemCount) {
            mutableStateOf(value = lazyTxHistoryItems)
        }

        val betweenItemsPadding = TangemTheme.dimens.spacing12
        val horizontalPadding = TangemTheme.dimens.spacing16
        val itemModifier = movableItemModifier
            .padding(top = betweenItemsPadding)
            .padding(horizontal = horizontalPadding)

        val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag(TestTags.WALLET_SCREEN),
            contentPadding = PaddingValues(
                bottom = TangemTheme.dimens.spacing92 + bottomBarHeight,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(
                // !!! Type of the key should be saveable via Bundle on Android !!!
                key = state.wallets.map { it.walletCardState.id.stringValue },
                contentType = state.wallets.map { it.walletCardState.id },
            ) {
                WalletsList(
                    lazyListState = walletsListState,
                    wallets = state.wallets.map(WalletState::walletCardState).toImmutableList(),
                    isBalanceHidden = state.isHidingMode,
                )
            }

            (selectedWallet as? WalletState.SingleCurrency)?.let {
                controlButtons(
                    configs = it.buttons,
                    selectedWalletIndex = selectedWalletIndex,
                    modifier = movableItemModifier.padding(top = betweenItemsPadding),
                )
            }

            notifications(configs = selectedWallet.warnings, modifier = itemModifier)

            (selectedWallet as? WalletState.SingleCurrency)?.let { walletState ->
                walletState.marketPriceBlockState?.let { marketPriceBlockState ->
                    marketPriceBlock(state = marketPriceBlockState, modifier = itemModifier)
                }
            }

            (selectedWallet as? WalletState.Visa.Content)?.let {
                depositButton(
                    modifier = itemModifier.fillMaxWidth(),
                    state = it.depositButtonState,
                )

                balancesAndLimitsBlock(
                    modifier = itemModifier,
                    state = it.balancesAndLimitBlockState,
                )
            }

            contentItems(
                state = selectedWallet,
                txHistoryItems = txHistoryItems,
                isBalanceHidden = state.isHidingMode,
                modifier = movableItemModifier,
            )

            organizeTokens(state = selectedWallet, itemModifier = itemModifier)
        }

        ShowBottomSheet(bottomSheetConfig = selectedWallet.bottomSheetConfig)

        WalletsListEffects(
            lazyListState = walletsListState,
            selectedWalletIndex = selectedWalletIndex,
            onWalletChange = state.onWalletChange,
            onSelectedWalletIndexSet = { selectedWalletIndex = it },
            isAutoScroll = isAutoScroll,
            onAutoScrollReset = onAutoScrollReset,
        )
    }

    if (marketsEntryComponent != null) {
        val bottomSheetState = remember { mutableStateOf(BottomSheetState.COLLAPSED) }

        var headerSize by remember { mutableStateOf(0.dp) }

        BaseScaffoldWithMarkets(
            state = state,
            selectedWallet = selectedWallet,
            snackbarHostState = snackbarHostState,
            bottomSheetHeaderHeightProvider = { headerSize },
            alertConfig = alertConfig,
            onBottomSheetStateChange = { bottomSheetState.value = it },
            bottomSheetContent = {
                marketsEntryComponent.BottomSheetContent(
                    bottomSheetState = bottomSheetState,
                    onHeaderSizeChange = { headerSize = it },
                    modifier = Modifier,
                )
            },
            content = scaffoldContent,
        )
    } else {
        BaseScaffold(
            state = state,
            selectedWallet = selectedWallet,
            snackbarHostState = snackbarHostState,
            content = scaffoldContent,
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun BaseScaffold(
    state: WalletScreenState,
    selectedWallet: WalletState,
    snackbarHostState: SnackbarHostState,
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = { WalletTopBar(config = state.topBarConfig) },
        contentWindowInsets = WindowInsetsZero,
        snackbarHost = {
            WalletSnackbarHost(
                snackbarHostState = snackbarHostState,
                event = state.event,
                modifier = Modifier.padding(bottom = TangemTheme.dimens.spacing16),
            )
        },
        floatingActionButton = {
            val manageTokensButtonConfig by rememberUpdatedState(
                newValue = (state.wallets[state.selectedWalletIndex] as? WalletState.MultiCurrency)
                    ?.manageTokensButtonConfig,
            )

            AnimatedVisibility(
                visible = manageTokensButtonConfig != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                val config = manageTokensButtonConfig ?: return@AnimatedVisibility

                ManageTokensButton(
                    modifier = Modifier.navigationBarsPadding(),
                    onClick = config.onClick,
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        containerColor = TangemTheme.colors.background.secondary,
        content = {
            val pullRefreshState = rememberPullRefreshState(
                refreshing = selectedWallet.pullToRefreshConfig.isRefreshing,
                onRefresh = {
                    selectedWallet.pullToRefreshConfig.onRefresh(WalletPullToRefreshConfig.ShowRefreshState(true))
                },
            )

            Box(
                modifier = Modifier
                    .pullRefresh(pullRefreshState)
                    .padding(it),
            ) {
                content()

                WalletPullToRefreshIndicator(
                    isRefreshing = selectedWallet.pullToRefreshConfig.isRefreshing,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                )

                BottomFade(Modifier.align(Alignment.BottomCenter))
            }
        },
    )
}

@Suppress("LongParameterList", "LongMethod")
@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
private inline fun BaseScaffoldWithMarkets(
    state: WalletScreenState,
    selectedWallet: WalletState,
    snackbarHostState: SnackbarHostState,
    bottomSheetHeaderHeightProvider: () -> Dp,
    crossinline bottomSheetContent: @Composable () -> Unit,
    alertConfig: WalletAlertState?,
    noinline onBottomSheetStateChange: (BottomSheetState) -> Unit,
    crossinline content: @Composable () -> Unit,
) {
    // show the bottom sheet if there is at least one multicurrency wallet
    val showManageTokensBottomSheet = remember(state.wallets) {
        state.wallets.any { it is WalletState.MultiCurrency }
    }
    val bottomSheetState = rememberSheetStateEnhanced(
        initialValue = if (showManageTokensBottomSheet) SheetValue.PartiallyExpanded else SheetValue.Hidden,
        confirmValueChange = remember(showManageTokensBottomSheet) {
            { sheetValue ->
                when {
                    sheetValue == SheetValue.Hidden && showManageTokensBottomSheet -> false
                    sheetValue != SheetValue.Hidden && !showManageTokensBottomSheet -> false
                    else -> true
                }
            }
        },
        skipHiddenState = showManageTokensBottomSheet,
    )

    val keyboardShown = keyboardAsState()

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = bottomSheetState,
        snackbarHostState = snackbarHostState,
    )

    val density = LocalDensity.current
    val bottomBarHeight = with(density) { WindowInsets.systemBars.getBottom(density = this).toDp() }
    val statusBarHeight = with(density) { WindowInsets.statusBars.getTop(density = this).toDp() }
    val peekHeight = bottomSheetHeaderHeightProvider() + handComposableComponentHeight + bottomBarHeight
    val maxHeight = LocalWindowSize.current.height

    val coroutineScope = rememberCoroutineScope()
    val backgroundPrimary = TangemTheme.colors.background.primary

    CompositionLocalProvider(
        LocalMainBottomSheetColor provides remember { mutableStateOf(backgroundPrimary) },
    ) {
        val backgroundColor = LocalMainBottomSheetColor.current

        BottomSheetStateEffects(
            bottomSheetState = bottomSheetState,
            showManageTokensBottomSheet = showManageTokensBottomSheet,
            alertConfig = alertConfig,
            keyboardShown = keyboardShown,
            onBottomSheetStateChange = onBottomSheetStateChange,
        )

        BottomSheetScaffold(
            snackbarHost = {
                WalletSnackbarHost(
                    snackbarHostState = it,
                    event = state.event,
                    modifier = Modifier
                        .padding(bottom = TangemTheme.dimens.spacing4)
                        .navigationBarsPadding(),
                )
            },
            containerColor = TangemTheme.colors.background.secondary,
            sheetContainerColor = backgroundColor.value,
            scaffoldState = scaffoldState,
            sheetPeekHeight = peekHeight,
            sheetDragHandle = {
                Hand(modifier = Modifier.background(color = backgroundColor.value))
            },
            sheetTonalElevation = 8.dp,
            sheetShadowElevation = 8.dp,
            sheetContent = {
                Box(
                    modifier = Modifier.sizeIn(maxHeight = maxHeight - statusBarHeight - handComposableComponentHeight),
                ) {
                    bottomSheetContent()
                }

                // hide bottom sheet when back pressed
                BackHandler(
                    keyboardShown.value is Keyboard.Closed &&
                        bottomSheetState.currentValue == SheetValue.Expanded,
                ) {
                    coroutineScope.launch { bottomSheetState.partialExpand() }
                }
            },
            content = { _ ->
                val pullRefreshState = rememberPullRefreshState(
                    refreshing = selectedWallet.pullToRefreshConfig.isRefreshing,
                    onRefresh = {
                        selectedWallet.pullToRefreshConfig.onRefresh(WalletPullToRefreshConfig.ShowRefreshState(true))
                    },
                )

                Box {
                    Column {
                        WalletTopBar(config = state.topBarConfig)
                        Box(
                            modifier = Modifier.pullRefresh(pullRefreshState),
                        ) {
                            content()

                            WalletPullToRefreshIndicator(
                                isRefreshing = selectedWallet.pullToRefreshConfig.isRefreshing,
                                state = pullRefreshState,
                                modifier = Modifier.align(Alignment.TopCenter),
                            )
                        }
                    }

                    BottomSheetScrim(
                        color = BottomSheetDefaults.ScrimColor,
                        visible = bottomSheetState.targetValue == SheetValue.Expanded || state.showMarketsOnboarding,
                        onDismissRequest = { coroutineScope.launch { bottomSheetState.partialExpand() } },
                    )

                    MarketsTooltip(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp)
                            .fillMaxWidth(fraction = 0.7f),
                        isVisible = state.showMarketsOnboarding,
                        availableHeight = maxHeight - statusBarHeight - bottomBarHeight,
                        bottomSheetState = bottomSheetState,
                    )
                }
            },
        )

        LaunchedEffect(state.showMarketsOnboarding, bottomSheetState.targetValue) {
            if (state.showMarketsOnboarding && bottomSheetState.targetValue == SheetValue.Expanded) {
                state.onDismissMarketsOnboarding()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MarketsTooltip(
    availableHeight: Dp,
    bottomSheetState: SheetState,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val tooltipOffset by remember {
        derivedStateOf {
            val bottomSheetOffset = try {
                // Can throw exception during the first composition
                with(density) { bottomSheetState.requireOffset().toDp() }
            } catch (e: Exception) {
                0.dp
            }

            bottomSheetOffset - availableHeight
        }
    }

    var visible by remember { mutableStateOf(value = false) }
    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(timeMillis = 300)
        }

        visible = isVisible
    }

    val slideOffset = 40.dp.toPx()
    AnimatedVisibility(
        modifier = modifier.absoluteOffset(y = tooltipOffset),
        visible = visible,
        enter = slideIn(
            animationSpec = spring(
                stiffness = Spring.StiffnessLow,
                visibilityThreshold = IntOffset.VisibilityThreshold,
            ),
            initialOffset = { _ -> IntOffset(y = -slideOffset.roundToInt(), x = 0) },
        ) + fadeIn(),
        exit = fadeOut(),
    ) {
        MarketsTooltipContent()
    }
}

@Composable
private fun MarketsTooltipContent(modifier: Modifier = Modifier) {
    val backgroundColor = TangemTheme.colors.background.primary
    val cornerRadius = CornerRadius(x = 14.dp.toPx())
    val tipDpSize = DpSize(width = 20.dp, height = 8.dp)

    Column(
        modifier = modifier
            .padding(bottom = tipDpSize.height)
            .drawBehind {
                val rect = size.toRect()
                val tipSize = tipDpSize.toSize()
                val tipRect = Rect(
                    offset = Offset(
                        x = rect.center.x - tipSize.center.x,
                        y = rect.bottom,
                    ),
                    size = tipSize,
                )
                drawRoundRect(color = backgroundColor, cornerRadius = cornerRadius)

                val tipPath = Path().apply {
                    moveTo(tipRect.topLeft)
                    lineTo(tipRect.bottomCenter)
                    lineTo(tipRect.topRight)
                }
                drawPath(color = backgroundColor, path = tipPath)
            }
            .padding(all = 12.dp),
        verticalArrangement = Arrangement.spacedBy(space = 4.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = stringResource(id = R.string.markets_tooltip_title),
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.primary1,
        )
        Text(
            text = stringResource(id = R.string.markets_tooltip_message),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.secondary,
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

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("CyclomaticComplexMethod", "MagicNumber", "LongMethod")
@Composable
private fun BottomSheetStateEffects(
    bottomSheetState: SheetState,
    showManageTokensBottomSheet: Boolean,
    alertConfig: WalletAlertState?,
    keyboardShown: State<Keyboard>,
    onBottomSheetStateChange: (BottomSheetState) -> Unit,
) {
    // Bottom sheet during initialization internally expand partially after its content was remeasured,
    // therefore initialValue = SheetValue.Hidden in rememberStandardBottomSheetState doesn't work as expected
    // so we have to manually restrict expansion in this case
    LaunchedEffect(bottomSheetState.targetValue, bottomSheetState.currentValue) {
        if (!showManageTokensBottomSheet &&
            (bottomSheetState.targetValue != SheetValue.Hidden || bottomSheetState.currentValue != SheetValue.Hidden)
        ) {
            bottomSheetState.hide()
        }
    }
    // react to changes in wallet list
    LaunchedEffect(showManageTokensBottomSheet) {
        when {
            showManageTokensBottomSheet && bottomSheetState.currentValue != SheetValue.PartiallyExpanded -> {
                bottomSheetState.partialExpand()
            }
            !showManageTokensBottomSheet && bottomSheetState.targetValue != SheetValue.Hidden -> {
                bottomSheetState.hide()
            }
        }
    }

    val systemUiController = rememberSystemUiController()
    val navigationBarColor = TangemTheme.colors.background.primary

    LaunchedEffect(key1 = bottomSheetState.targetValue, navigationBarColor) {
        when (bottomSheetState.targetValue) {
            SheetValue.Hidden,
            SheetValue.Expanded,
            -> systemUiController.setNavigationBarColor(
                color = Color.Transparent,
                darkIcons = navigationBarColor.luminance() > 0.5f,
                navigationBarContrastEnforced = true,
            )
            SheetValue.PartiallyExpanded,
            -> systemUiController.setNavigationBarColor(navigationBarColor)
        }
    }

    DisposableEffect(showManageTokensBottomSheet) {
        onDispose {
            if (showManageTokensBottomSheet) {
                systemUiController.setNavigationBarColor(
                    color = Color.Transparent,
                    darkIcons = navigationBarColor.luminance() > 0.5f,
                    navigationBarContrastEnforced = false,
                )
            }
        }
    }

    // expand bottom sheet when keyboard appears
    LaunchedEffect(keyboardShown.value is Keyboard.Opened) {
        if (keyboardShown.value is Keyboard.Opened && alertConfig == null) {
            bottomSheetState.expand()
        }
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    // hide keyboard when bottom sheet is about to be hidden
    LaunchedEffect(Unit) {
        snapshotFlow {
            bottomSheetState.currentValue == SheetValue.Expanded &&
                bottomSheetState.targetValue == SheetValue.PartiallyExpanded
        }.collect { sheetHasBeenHidden ->
            if (sheetHasBeenHidden) {
                keyboardController?.hide()
            }
        }
    }

    val isSheetHidden = bottomSheetState.targetValue == SheetValue.PartiallyExpanded
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

/**
 * Use a standard method when this is fixed https://issuetracker.google.com/issues/314796718
 * Current material3 version: 1.2.0
 */
@Composable
@ExperimentalMaterial3Api
private fun rememberSheetStateEnhanced(
    skipPartiallyExpanded: Boolean = false,
    confirmValueChange: (SheetValue) -> Boolean = { true },
    initialValue: SheetValue = SheetValue.Hidden,
    skipHiddenState: Boolean = false,
): SheetState {
    val density = LocalDensity.current
    return remember(initialValue, skipPartiallyExpanded, confirmValueChange, skipHiddenState) {
        SheetState(
            skipPartiallyExpanded = skipPartiallyExpanded,
            density = density,
            initialValue = initialValue,
            confirmValueChange = confirmValueChange,
            skipHiddenState = skipHiddenState,
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

@Composable
private fun ManageTokensButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    PrimaryButton(
        text = stringResource(id = R.string.main_manage_tokens),
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = TangemTheme.dimens.spacing16),
    )
}

internal fun LazyListScope.organizeTokens(state: WalletState, itemModifier: Modifier) {
    (state as? WalletState.MultiCurrency)?.let {
        (state.tokensListState as? WalletTokensListState.ContentState)?.let {
            it.organizeTokensButtonConfig?.let { config ->
                organizeTokensButton(
                    modifier = itemModifier,
                    isEnabled = config.isEnabled,
                    onClick = config.onClick,
                )
            }
        }
    }
}

@Composable
private fun ShowBottomSheet(bottomSheetConfig: TangemBottomSheetConfig?) {
    if (bottomSheetConfig != null) {
        when (bottomSheetConfig.content) {
            is WalletBottomSheetConfig -> WalletBottomSheet(config = bottomSheetConfig)
            is TokenReceiveBottomSheetConfig -> TokenReceiveBottomSheet(config = bottomSheetConfig)
            is ActionsBottomSheetConfig -> TokenActionsBottomSheet(config = bottomSheetConfig)
            is ChooseAddressBottomSheetConfig -> ChooseAddressBottomSheet(config = bottomSheetConfig)
            is BalancesAndLimitsBottomSheetConfig -> BalancesAndLimitsBottomSheet(config = bottomSheetConfig)
            is VisaTxDetailsBottomSheetConfig -> VisaTxDetailsBottomSheet(config = bottomSheetConfig)
            is PushNotificationsBottomSheetConfig -> PushNotificationsBottomSheet(config = bottomSheetConfig)
        }
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WalletScreen_Preview(@PreviewParameter(WalletScreenPreviewProvider::class) data: WalletScreenState) {
    TangemThemePreview {
        WalletScreen(
            state = data,
            marketsEntryComponent = null,
        )
    }
}

private class WalletScreenPreviewProvider : PreviewParameterProvider<WalletScreenState> {
    override val values: Sequence<WalletScreenState>
        get() = sequenceOf(
            walletScreenState,
            walletScreenState.copy(selectedWalletIndex = 1),
        )
}
// endregion
