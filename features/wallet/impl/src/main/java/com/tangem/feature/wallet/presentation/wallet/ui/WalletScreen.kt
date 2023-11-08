package com.tangem.feature.wallet.presentation.wallet.ui

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.paging.compose.collectAsLazyPagingItems
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.bottomsheets.chooseaddress.ChooseAddressBottomSheet
import com.tangem.core.ui.components.bottomsheets.chooseaddress.ChooseAddressBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.tokenreceive.TokenReceiveBottomSheet
import com.tangem.core.ui.components.bottomsheets.tokenreceive.TokenReceiveBottomSheetConfig
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.wallet.state.*
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletBottomSheetConfig
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletPullToRefreshConfig
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletTokensListState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletTokensListState.OrganizeTokensButtonState
import com.tangem.feature.wallet.presentation.wallet.ui.components.TokenActionsBottomSheet
import com.tangem.feature.wallet.presentation.wallet.ui.components.WalletsList
import com.tangem.feature.wallet.presentation.wallet.ui.components.common.*
import com.tangem.feature.wallet.presentation.wallet.ui.components.multicurrency.organizeTokensButton
import com.tangem.feature.wallet.presentation.wallet.ui.components.singlecurrency.controlButtons
import com.tangem.feature.wallet.presentation.wallet.ui.components.singlecurrency.marketPriceBlock
import com.tangem.feature.wallet.presentation.wallet.ui.utils.changeWalletAnimator

/**
 * Wallet screen
 *
 * @param state screen state
 *
[REDACTED_AUTHOR]
 */
@Composable
internal fun WalletScreen(state: WalletState) {
    BackHandler(onBack = state.onBackClick)

    when (state) {
        is WalletState.ContentState -> {
            val walletsListState = rememberLazyListState(
                initialFirstVisibleItemIndex = state.walletsListConfig.selectedWalletIndex,
            )
            val snackbarHostState = remember { SnackbarHostState() }
            val isAutoScroll = remember { mutableStateOf(value = false) }

            WalletContent(
                state = state,
                walletsListState = walletsListState,
                snackbarHostState = snackbarHostState,
                isAutoScroll = isAutoScroll,
                onAutoScrollReset = { isAutoScroll.value = false },
            )

            var alertConfig by remember { mutableStateOf<WalletAlertState?>(value = null) }

            WalletEventEffect(
                walletsListState = walletsListState,
                snackbarHostState = snackbarHostState,
                event = state.event,
                onAutoScrollSet = { isAutoScroll.value = true },
                onAlertConfigSet = { alertConfig = it },
            )

            alertConfig?.let {
                WalletAlert(state = it, onDismiss = { alertConfig = null })
            }
        }
        is WalletState.Initial -> Unit
    }
}

@Composable
private fun WalletContent(
    state: WalletState.ContentState,
    walletsListState: LazyListState,
    snackbarHostState: SnackbarHostState,
    isAutoScroll: State<Boolean>,
    onAutoScrollReset: () -> Unit,
) {
    BaseScaffold(state = state, snackbarHostState) { scaffoldPaddings ->
        val movableItemModifier = Modifier.changeWalletAnimator(walletsListState)

        UpdatableContainer(
            pullToRefreshConfig = state.pullToRefreshConfig,
            modifier = Modifier.padding(paddingValues = scaffoldPaddings),
        ) {
            val txHistoryItems = if (state is WalletSingleCurrencyState &&
                state.txHistoryState is TxHistoryState.Content
            ) {
                (state.txHistoryState as? TxHistoryState.Content)?.contentItems?.collectAsLazyPagingItems()
            } else {
                null
            }

            val betweenItemsPadding = TangemTheme.dimens.spacing14
            val horizontalPadding = TangemTheme.dimens.spacing16
            val itemModifier = movableItemModifier
                .padding(top = betweenItemsPadding)
                .padding(horizontal = horizontalPadding)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = TangemTheme.dimens.spacing8,
                    bottom = TangemTheme.dimens.spacing92,
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item {
                    WalletsList(
                        config = state.walletsListConfig,
                        lazyListState = walletsListState,
                        isBalanceHidden = state.isBalanceHidden,
                    )
                }

                if (state is WalletSingleCurrencyState) {
                    controlButtons(
                        configs = state.buttons,
                        selectedWalletIndex = state.walletsListConfig.selectedWalletIndex,
                        modifier = movableItemModifier.padding(top = betweenItemsPadding),
                    )
                }

                notifications(configs = state.notifications, modifier = itemModifier)

                if (state is WalletSingleCurrencyState.Content) {
                    marketPriceBlock(state = state.marketPriceBlockState, modifier = itemModifier)
                }

                contentItems(
                    state = state,
                    txHistoryItems = txHistoryItems,
                    isBalanceHidden = state.isBalanceHidden,
                    modifier = movableItemModifier,
                )

                organizeTokens(state = state, itemModifier = itemModifier)
            }
        }
    }

    WalletBottomSheets(state = state)

    WalletsListEffects(
        lazyListState = walletsListState,
        walletsListConfig = state.walletsListConfig,
        isAutoScroll = isAutoScroll,
        onAutoScrollReset = onAutoScrollReset,
    )
}

internal fun LazyListScope.organizeTokens(state: WalletState.ContentState, itemModifier: Modifier) {
    if (state is WalletMultiCurrencyState) {
        val contentTokenListState = state.tokensListState as? WalletTokensListState.ContentState
        val organizeTokensButton = contentTokenListState?.organizeTokensButton

        if (organizeTokensButton is OrganizeTokensButtonState.Visible) {
            organizeTokensButton(
                modifier = itemModifier,
                isEnabled = organizeTokensButton.isEnabled,
                onClick = organizeTokensButton.onClick,
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun UpdatableContainer(
    pullToRefreshConfig: WalletPullToRefreshConfig,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val pullRefreshState = rememberPullRefreshState(
        refreshing = pullToRefreshConfig.isRefreshing,
        onRefresh = pullToRefreshConfig.onRefresh,
    )

    Box(modifier = modifier.pullRefresh(pullRefreshState)) {
        content()

        WalletPullToRefreshIndicator(
            isRefreshing = pullToRefreshConfig.isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun BaseScaffold(
    state: WalletState.ContentState,
    snackbarHostState: SnackbarHostState,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = { WalletTopBar(config = state.topBarConfig) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            if (state is WalletMultiCurrencyState.Content && state.isManageTokensAvailable) {
                ManageTokensButton(onManageTokensClick = state.onManageTokensClick)
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        containerColor = TangemTheme.colors.background.secondary,
        content = content,
    )
}

@Composable
private fun ManageTokensButton(onManageTokensClick: () -> Unit) {
    PrimaryButton(
        text = stringResource(id = R.string.main_manage_tokens),
        onClick = onManageTokensClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TangemTheme.dimens.spacing16),
    )
}

@Composable
private fun WalletBottomSheets(state: WalletState) {
    val bottomSheetConfig = (state as? WalletState.ContentState)?.bottomSheetConfig
    if (bottomSheetConfig != null) {
        when (bottomSheetConfig.content) {
            is WalletBottomSheetConfig -> WalletBottomSheet(config = bottomSheetConfig)
            is TokenReceiveBottomSheetConfig -> TokenReceiveBottomSheet(config = bottomSheetConfig)
            is ActionsBottomSheetConfig -> TokenActionsBottomSheet(config = bottomSheetConfig)
            is ChooseAddressBottomSheetConfig -> ChooseAddressBottomSheet(config = bottomSheetConfig)
        }
    }
}

// region Preview
@Preview
@Composable
private fun WalletScreenPreview_Light(@PreviewParameter(WalletScreenParameterProvider::class) state: WalletState) {
    TangemTheme {
        WalletScreen(state = state)
    }
}

@Preview
@Composable
private fun WalletScreenPreview_Dark(@PreviewParameter(WalletScreenParameterProvider::class) state: WalletState) {
    TangemTheme(isDark = true) {
        WalletScreen(state = state)
    }
}

private class WalletScreenParameterProvider : CollectionPreviewParameterProvider<WalletState>(
    collection = listOf(
        WalletPreviewData.multicurrencyWalletScreenState,
        WalletPreviewData.singleWalletScreenState,
    ),
)
// endregion Preview