package com.tangem.feature.wallet.presentation.wallet.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.paging.compose.collectAsLazyPagingItems
import com.tangem.core.ui.components.BottomFade
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.chooseaddress.ChooseAddressBottomSheet
import com.tangem.core.ui.components.bottomsheets.chooseaddress.ChooseAddressBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.tokenreceive.TokenReceiveBottomSheet
import com.tangem.core.ui.components.bottomsheets.tokenreceive.TokenReceiveBottomSheetConfig
import com.tangem.core.ui.components.snackbar.CopiedTextSnackbar
import com.tangem.core.ui.components.snackbar.TangemSnackbar
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.event.StateEvent
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.TestTags
import com.tangem.core.ui.utils.WindowInsetsZero
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
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun WalletScreen(state: WalletScreenState) {
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

    BaseScaffold(
        state = state,
        selectedWallet = selectedWallet,
        snackbarHostState = snackbarHostState,
    ) {
        scaffoldContent()
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
            val manageTokensButtonConfig by remember(state.selectedWalletIndex) {
                mutableStateOf(
                    (state.wallets[state.selectedWalletIndex] as? WalletState.MultiCurrency)?.manageTokensButtonConfig,
                )
            }

            manageTokensButtonConfig?.let {
                ManageTokensButton(
                    modifier = Modifier.navigationBarsPadding(),
                    onClick = it.onClick,
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