package com.tangem.feature.wallet.presentation.wallet.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.tangem.common.ui.expressStatus.ExpressStatusBottomSheet
import com.tangem.common.ui.expressStatus.ExpressStatusBottomSheetConfig
import com.tangem.common.ui.expressStatus.expressTransactionsItems
import com.tangem.core.ui.components.atoms.Hand
import com.tangem.core.ui.components.atoms.handComposableComponentHeight
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.chooseaddress.ChooseAddressBottomSheet
import com.tangem.core.ui.components.bottomsheets.chooseaddress.ChooseAddressBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.tokenreceive.TokenReceiveBottomSheet
import com.tangem.core.ui.components.bottomsheets.tokenreceive.TokenReceiveBottomSheetConfig
import com.tangem.core.ui.components.containers.pullToRefresh.TangemPullToRefreshContainer
import com.tangem.core.ui.components.rememberIsKeyboardVisible
import com.tangem.core.ui.components.sheetscaffold.*
import com.tangem.core.ui.components.snackbar.CopiedTextSnackbar
import com.tangem.core.ui.components.snackbar.TangemSnackbar
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.event.StateEvent
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.core.ui.res.LocalWindowSize
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.TestTags
import com.tangem.core.ui.utils.lineTo
import com.tangem.core.ui.utils.moveTo
import com.tangem.core.ui.utils.toPx
import com.tangem.feature.wallet.presentation.wallet.state.model.ActionsBottomSheetConfig
import com.tangem.feature.wallet.presentation.wallet.state.model.BalancesAndLimitsBottomSheetConfig
import com.tangem.feature.wallet.presentation.wallet.state.model.VisaTxDetailsBottomSheetConfig
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletAlertState
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.preview.WalletScreenPreviewData.walletScreenState
import com.tangem.feature.wallet.presentation.wallet.state.model.*
import com.tangem.feature.wallet.presentation.wallet.state.model.holder.TxHistoryStateHolder
import com.tangem.feature.wallet.presentation.wallet.ui.components.PushNotificationsBottomSheet
import com.tangem.feature.wallet.presentation.wallet.ui.components.TokenActionsBottomSheet
import com.tangem.feature.wallet.presentation.wallet.ui.components.WalletsList
import com.tangem.feature.wallet.presentation.wallet.ui.components.common.*
import com.tangem.feature.wallet.presentation.wallet.ui.components.common.actions
import com.tangem.feature.wallet.presentation.wallet.ui.components.multicurrency.organizeTokensButton
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
internal fun WalletScreen(state: WalletScreenState, marketsEntryComponent: MarketsEntryComponent) {
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
        walletsListState = walletsListState,
        snackbarHostState = snackbarHostState,
        event = state.event,
        onAutoScrollSet = { isAutoScroll.value = true },
        onAlertConfigSet = { alertConfig = it },
    )
}

@Suppress("LongMethod", "LongParameterList", "CyclomaticComplexMethod")
@Composable
private fun WalletContent(
    state: WalletScreenState,
    walletsListState: LazyListState,
    snackbarHostState: SnackbarHostState,
    isAutoScroll: State<Boolean>,
    marketsEntryComponent: MarketsEntryComponent,
    alertConfig: WalletAlertState?,
    onAutoScrollReset: () -> Unit,
) {
    /*
     * Don't pass key to remember, because it will brake scroll animation.
     * selectedWalletIndex will be changed in WalletsListEffects.
     */
    val selectedWalletIndex by remember(state.selectedWalletIndex) { mutableIntStateOf(state.selectedWalletIndex) }
    val selectedWallet = state.wallets.getOrElse(selectedWalletIndex) { state.wallets[state.selectedWalletIndex] }

    val listState = rememberLazyListState()

    val scaffoldContent: @Composable (PaddingValues?) -> Unit = { paddingValues ->
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

        val marketHintAproxHeight = with(LocalDensity.current) {
            TangemTheme.typography.caption2.lineHeight.toDp() * 2
        } + 40.dp

        val contentPadding = paddingValues?.let {
            PaddingValues(
                bottom = it.calculateBottomPadding() + marketHintAproxHeight + 52.dp,
            )
        } ?: PaddingValues(bottom = TangemTheme.dimens.spacing92 + bottomBarHeight)

        LazyColumn(
            modifier = Modifier.testTag(TestTags.MAIN_SCREEN),
            state = listState,
            contentPadding = contentPadding,
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

            when (selectedWallet) {
                is WalletState.MultiCurrency,
                is WalletState.Visa,
                -> {
                    actions(
                        actions = selectedWallet.buttons,
                        selectedWalletIndex = selectedWalletIndex,
                        modifier = movableItemModifier.padding(top = betweenItemsPadding),
                    )
                }
                is WalletState.SingleCurrency -> {
                    lazyActions(
                        actions = selectedWallet.buttons,
                        selectedWalletIndex = selectedWalletIndex,
                        modifier = movableItemModifier.padding(top = betweenItemsPadding),
                    )
                }
            }

            notifications(configs = selectedWallet.warnings, modifier = itemModifier)

            (selectedWallet as? WalletState.SingleCurrency)?.let { walletState ->
                walletState.marketPriceBlockState?.let { marketPriceBlockState ->
                    marketPriceBlock(state = marketPriceBlockState, modifier = itemModifier)
                }
                if (walletState is WalletState.SingleCurrency.Content) {
                    expressTransactionsItems(
                        expressTxs = walletState.expressTxsToDisplay,
                        modifier = itemModifier,
                    )
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

    val bottomSheetState = remember { mutableStateOf(BottomSheetState.COLLAPSED) }

    var headerSize by remember { mutableStateOf(0.dp) }

    BaseScaffoldWithMarkets(
        state = state,
        listState = listState,
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
}

@Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private inline fun BaseScaffoldWithMarkets(
    state: WalletScreenState,
    listState: LazyListState,
    selectedWallet: WalletState,
    snackbarHostState: SnackbarHostState,
    bottomSheetHeaderHeightProvider: () -> Dp,
    crossinline bottomSheetContent: @Composable () -> Unit,
    alertConfig: WalletAlertState?,
    noinline onBottomSheetStateChange: (BottomSheetState) -> Unit,
    crossinline content: @Composable (PaddingValues) -> Unit,
) {
    val bottomSheetState = rememberTangemStandardBottomSheetState()

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
    val backgroundPrimary = TangemTheme.colors.background.primary

    val showMarketsHint by remember {
        derivedStateOf {
            // Show hint only when there are items in the list
            // and when there a no items to scroll
            listState.layoutInfo.totalItemsCount > 0 &&
                !listState.canScrollBackward && !listState.canScrollForward ||
                listState.canScrollBackward && !listState.canScrollForward
        }
    }

    CompositionLocalProvider(
        LocalMainBottomSheetColor provides remember { mutableStateOf(backgroundPrimary) },
    ) {
        val backgroundColor = LocalMainBottomSheetColor.current
        var isSearchFieldFocused by remember { mutableStateOf(false) }

        BottomSheetStateEffects(
            bottomSheetState = bottomSheetState,
            alertConfig = alertConfig,
            onBottomSheetStateChange = onBottomSheetStateChange,
            isSearchFieldFocused = isSearchFieldFocused,
        )

        TangemBottomSheetScaffold(
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
                    modifier = Modifier.sizeIn(maxHeight = maxHeight - statusBarHeight),
                ) {
                    Hand(Modifier.drawBehind { drawRect(backgroundColor.value) })

                    Box(
                        modifier = Modifier
                            // expand bottom sheet when clicked on the header
                            .clickable(
                                enabled = bottomSheetState.currentValue == TangemSheetValue.PartiallyExpanded,
                                indication = null,
                                interactionSource = null,
                            ) {
                                coroutineScope.launch { bottomSheetState.expand() }
                            }
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
                    MarketsHint(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = peekHeight + 12.dp)
                            .fillMaxWidth(fraction = .4f),
                        isVisible = showMarketsHint,
                    )

                    Column {
                        WalletTopBar(config = state.topBarConfig)
                        TangemPullToRefreshContainer(config = selectedWallet.pullToRefreshConfig) {
                            content(paddingValues)
                        }
                    }

                    BottomSheetScrim(
                        color = if (state.showMarketsOnboarding) {
                            Color.Black.copy(alpha = .65f)
                        } else {
                            BottomSheetDefaults.ScrimColor
                        },
                        visible = bottomSheetState.targetValue == TangemSheetValue.Expanded ||
                            state.showMarketsOnboarding,
                        onDismissRequest = {
                            coroutineScope.launch { bottomSheetState.partialExpand() }
                            state.onDismissMarketsOnboarding()
                        },
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
            if (state.showMarketsOnboarding && bottomSheetState.targetValue == TangemSheetValue.Expanded) {
                state.onDismissMarketsOnboarding()
            }
        }
    }
}

@Composable
private fun MarketsTooltip(
    availableHeight: Dp,
    bottomSheetState: TangemSheetState,
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
        modifier = modifier.offset { IntOffset(x = 0, y = tooltipOffset.roundToPx()) },
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
internal fun MarketsHint(isVisible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        modifier = modifier,
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(durationMillis = 500)),
        exit = fadeOut(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(space = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResourceSafe(R.string.markets_hint),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
                textAlign = TextAlign.Center,
            )
            Icon(
                modifier = Modifier.size(size = 24.dp),
                painter = painterResource(id = R.drawable.ic_chevron_24),
                tint = TangemTheme.colors.icon.informative,
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun MarketsTooltipContent(modifier: Modifier = Modifier) {
    val backgroundColor = TangemTheme.colors.background.action
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
            text = stringResourceSafe(id = R.string.markets_tooltip_title),
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.primary1,
        )
        Text(
            text = stringResourceSafe(id = R.string.markets_tooltip_message),
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

@Suppress("CyclomaticComplexMethod", "MagicNumber", "LongMethod")
@Composable
private fun BottomSheetStateEffects(
    bottomSheetState: TangemSheetState,
    alertConfig: WalletAlertState?,
    onBottomSheetStateChange: (BottomSheetState) -> Unit,
    isSearchFieldFocused: Boolean,
) {
    val systemUiController = rememberSystemUiController()
    val navigationBarColor = TangemTheme.colors.background.primary

    LaunchedEffect(navigationBarColor) {
        delay(timeMillis = 100)
        when (bottomSheetState.currentValue) {
            TangemSheetValue.Hidden,
            TangemSheetValue.Expanded,
            -> systemUiController.setNavigationBarColor(
                color = Color.Transparent,
                darkIcons = navigationBarColor.luminance() > 0.5f,
                navigationBarContrastEnforced = true,
            )
            TangemSheetValue.PartiallyExpanded,
            -> systemUiController.setNavigationBarColor(navigationBarColor)
        }
    }

    LaunchedEffect(bottomSheetState.targetValue, navigationBarColor) {
        when (bottomSheetState.targetValue) {
            TangemSheetValue.Hidden,
            TangemSheetValue.Expanded,
            -> systemUiController.setNavigationBarColor(
                color = Color.Transparent,
                darkIcons = navigationBarColor.luminance() > 0.5f,
                navigationBarContrastEnforced = true,
            )
            TangemSheetValue.PartiallyExpanded,
            -> systemUiController.setNavigationBarColor(navigationBarColor)
        }
    }

    // make navigation bar transparent when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            systemUiController.setNavigationBarColor(
                color = Color.Transparent,
                darkIcons = navigationBarColor.luminance() > 0.5f,
                navigationBarContrastEnforced = false,
            )
        }
    }

    // expand bottom sheet when keyboard appears
    val isKeyboardVisible by rememberIsKeyboardVisible()

    LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible && alertConfig == null && isSearchFieldFocused) {
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
            is ExpressStatusBottomSheetConfig -> ExpressStatusBottomSheet(config = bottomSheetConfig)
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
            marketsEntryComponent = object : MarketsEntryComponent {
                @Composable
                override fun BottomSheetContent(
                    bottomSheetState: State<BottomSheetState>,
                    onHeaderSizeChange: (Dp) -> Unit,
                    modifier: Modifier,
                ) {
                    Text("Markets Content")
                }
            },
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