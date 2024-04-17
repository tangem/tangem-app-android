package com.tangem.feature.wallet.presentation.wallet.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.tangem.core.ui.components.Keyboard
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SystemBarsEffect
import com.tangem.core.ui.components.atoms.Hand
import com.tangem.core.ui.components.atoms.handComposableComponentHeight
import com.tangem.core.ui.components.bottomsheets.chooseaddress.ChooseAddressBottomSheet
import com.tangem.core.ui.components.bottomsheets.chooseaddress.ChooseAddressBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.tokenreceive.TokenReceiveBottomSheet
import com.tangem.core.ui.components.bottomsheets.tokenreceive.TokenReceiveBottomSheetConfig
import com.tangem.core.ui.components.keyboardAsState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.test.TestTags
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.*
import com.tangem.feature.wallet.presentation.wallet.state.model.holder.TxHistoryStateHolder
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
import com.tangem.features.managetokens.navigation.ExpandableState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

@Composable
internal fun WalletScreen(
    state: WalletScreenState,
    bottomSheetHeaderHeightProvider: () -> Dp,
    bottomSheetContent: @Composable () -> Unit,
) {
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
        bottomSheetHeaderHeightProvider = bottomSheetHeaderHeightProvider,
        bottomSheetContent = bottomSheetContent,
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

@Suppress("LongMethod", "LongParameterList")
@Composable
private fun WalletContent(
    state: WalletScreenState,
    walletsListState: LazyListState,
    snackbarHostState: SnackbarHostState,
    isAutoScroll: State<Boolean>,
    bottomSheetHeaderHeightProvider: () -> Dp,
    onAutoScrollReset: () -> Unit,
    bottomSheetContent: @Composable () -> Unit,
    alertConfig: WalletAlertState?,
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

        val betweenItemsPadding = TangemTheme.dimens.spacing14
        val horizontalPadding = TangemTheme.dimens.spacing16
        val itemModifier = movableItemModifier
            .padding(top = betweenItemsPadding)
            .padding(horizontal = horizontalPadding)

        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag(TestTags.WALLET_SCREEN),
            contentPadding = PaddingValues(
                top = TangemTheme.dimens.spacing8,
                bottom = TangemTheme.dimens.spacing92,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(
                key = state.wallets.map { it.walletCardState.id },
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

        val bottomSheetConfig = selectedWallet.bottomSheetConfig
        if (bottomSheetConfig != null) {
            when (bottomSheetConfig.content) {
                is WalletBottomSheetConfig -> WalletBottomSheet(config = bottomSheetConfig)
                is TokenReceiveBottomSheetConfig -> TokenReceiveBottomSheet(config = bottomSheetConfig)
                is ActionsBottomSheetConfig -> TokenActionsBottomSheet(config = bottomSheetConfig)
                is ChooseAddressBottomSheetConfig -> ChooseAddressBottomSheet(config = bottomSheetConfig)
                is BalancesAndLimitsBottomSheetConfig -> BalancesAndLimitsBottomSheet(config = bottomSheetConfig)
                is VisaTxDetailsBottomSheetConfig -> VisaTxDetailsBottomSheet(config = bottomSheetConfig)
            }
        }

        WalletsListEffects(
            lazyListState = walletsListState,
            selectedWalletIndex = selectedWalletIndex,
            onWalletChange = state.onWalletChange,
            onSelectedWalletIndexSet = { selectedWalletIndex = it },
            isAutoScroll = isAutoScroll,
            onAutoScrollReset = onAutoScrollReset,
        )
    }

    if (state.manageTokenRedesignToggle) {
        BaseScaffoldManageTokenRedesign(
            state = state,
            selectedWallet = selectedWallet,
            snackbarHostState = snackbarHostState,
            bottomSheetHeaderHeightProvider = bottomSheetHeaderHeightProvider,
            bottomSheetContent = bottomSheetContent,
            alertConfig = alertConfig,
        ) {
            scaffoldContent()
        }
    } else {
        BaseScaffold(
            state = state,
            selectedWallet = selectedWallet,
            snackbarHostState = snackbarHostState,
        ) {
            scaffoldContent()
        }
    }
}

@Suppress("LongParameterList", "LongMethod")
@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun BaseScaffoldManageTokenRedesign(
    state: WalletScreenState,
    selectedWallet: WalletState,
    snackbarHostState: SnackbarHostState,
    bottomSheetHeaderHeightProvider: () -> Dp,
    bottomSheetContent: @Composable () -> Unit,
    alertConfig: WalletAlertState?,
    content: @Composable () -> Unit,
) {
    // show the bottom sheet if there is at least one multicurrency wallet
    val showManageTokensBottomSheet = remember(state.wallets) {
        state.wallets.any { it is WalletState.MultiCurrency }
    }
    val bottomSheetState = rememberSheetStateEnhanced(
        initialValue = if (showManageTokensBottomSheet) SheetValue.PartiallyExpanded else SheetValue.Hidden,
        confirmValueChange = { sheetValue ->
            when {
                sheetValue == SheetValue.Hidden && showManageTokensBottomSheet -> false
                sheetValue != SheetValue.Hidden && !showManageTokensBottomSheet -> false
                else -> true
            }
        },
        skipHiddenState = showManageTokensBottomSheet,
    )

    val keyboardShown = keyboardAsState()

    BottomSheetStateEffects(
        bottomSheetState = bottomSheetState,
        state = state,
        showManageTokensBottomSheet = showManageTokensBottomSheet,
        alertConfig = alertConfig,
        keyboardShown = keyboardShown,
    )

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = bottomSheetState,
        snackbarHostState = snackbarHostState,
    )

    val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }
    val statusBarHeight = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }
    val peekHeight = bottomSheetHeaderHeightProvider() + handComposableComponentHeight + bottomBarHeight

    val coroutineScope = rememberCoroutineScope()

    BottomSheetScaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        containerColor = TangemTheme.colors.background.secondary,
        sheetContainerColor = TangemTheme.colors.background.primary,
        scaffoldState = scaffoldState,
        sheetPeekHeight = peekHeight,
        sheetDragHandle = {
            Hand(modifier = Modifier.background(color = TangemTheme.colors.background.primary))
        },
        sheetContent = {
            BoxWithConstraints {
                Box(
                    modifier = Modifier
                        .sizeIn(maxHeight = maxHeight - statusBarHeight)
                        .align(Alignment.BottomCenter),
                ) {
                    bottomSheetContent()
                }
            }

            // hide bottom sheet when back pressed
            BackHandler(
                keyboardShown.value is Keyboard.Closed &&
                    bottomSheetState.currentValue == SheetValue.Expanded,
            ) {
                coroutineScope.launch { bottomSheetState.partialExpand() }
            }
        },
        content = { paddingValues ->
            val pullRefreshState = rememberPullRefreshState(
                refreshing = selectedWallet.pullToRefreshConfig.isRefreshing,
                onRefresh = selectedWallet.pullToRefreshConfig.onRefresh,
            )

            Column(
                modifier = Modifier.padding(paddingValues),
            ) {
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
                visible = bottomSheetState.targetValue == SheetValue.Expanded,
                onDismissRequest = { coroutineScope.launch { bottomSheetState.partialExpand() } },
            )
        },
    )
}

@Composable
private fun BottomSheetScrim(color: Color, visible: Boolean, onDismissRequest: () -> Unit) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = TweenSpec(),
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
@Suppress("CyclomaticComplexMethod")
@Composable
private fun BottomSheetStateEffects(
    bottomSheetState: SheetState,
    state: WalletScreenState,
    showManageTokensBottomSheet: Boolean,
    alertConfig: WalletAlertState?,
    keyboardShown: State<Keyboard>,
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
    val navigationBarColorWithout = TangemTheme.colors.background.secondary

    SystemBarsEffect {
        if (showManageTokensBottomSheet) {
            setNavigationBarColor(navigationBarColor)
        }
    }
    DisposableEffect(
        showManageTokensBottomSheet,
    ) {
        onDispose {
            if (showManageTokensBottomSheet) {
                systemUiController.setNavigationBarColor(navigationBarColorWithout)
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
        if (isSheetHidden) {
            state.manageTokensExpandableState.value = ExpandableState.COLLAPSED
        } else {
            state.manageTokensExpandableState.value = ExpandableState.EXPANDED
        }
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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            val manageTokensButtonConfig by remember(state.selectedWalletIndex) {
                mutableStateOf(
                    (state.wallets[state.selectedWalletIndex] as? WalletState.MultiCurrency)?.manageTokensButtonConfig,
                )
            }

            manageTokensButtonConfig?.let { ManageTokensButton(onClick = it.onClick) }
        },
        floatingActionButtonPosition = FabPosition.Center,
        containerColor = TangemTheme.colors.background.secondary,
        content = {
            val pullRefreshState = rememberPullRefreshState(
                refreshing = selectedWallet.pullToRefreshConfig.isRefreshing,
                onRefresh = selectedWallet.pullToRefreshConfig.onRefresh,
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
            }
        },
    )
}

@Composable
private fun ManageTokensButton(onClick: () -> Unit) {
    PrimaryButton(
        text = stringResource(id = R.string.main_manage_tokens),
        onClick = onClick,
        modifier = Modifier
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
