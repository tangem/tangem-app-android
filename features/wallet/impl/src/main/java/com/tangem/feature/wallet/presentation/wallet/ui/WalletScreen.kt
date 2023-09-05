package com.tangem.feature.wallet.presentation.wallet.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.paging.compose.collectAsLazyPagingItems
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.wallet.state.WalletMultiCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletSingleCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletTokensListState
import com.tangem.feature.wallet.presentation.wallet.ui.components.TokenActionsBottomSheet
import com.tangem.feature.wallet.presentation.wallet.ui.components.WalletsList
import com.tangem.feature.wallet.presentation.wallet.ui.components.common.*
import com.tangem.feature.wallet.presentation.wallet.ui.components.multicurrency.organizeButton
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
        is WalletState.ContentState -> WalletContent(state = state)
        is WalletState.Initial -> Unit
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun WalletContent(state: WalletState.ContentState) {
    val walletsListState = rememberLazyListState()

    BaseScaffold(state = state) { scaffoldPaddings ->
        val movableItemModifier = Modifier.changeWalletAnimator(walletsListState)
        val pullRefreshState = rememberPullRefreshState(
            refreshing = state.pullToRefreshConfig.isRefreshing,
            onRefresh = state.pullToRefreshConfig.onRefresh,
        )

        Box(
            modifier = Modifier
                .padding(paddingValues = scaffoldPaddings)
                .pullRefresh(pullRefreshState),
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
                    WalletsList(config = state.walletsListConfig, lazyListState = walletsListState)
                }

                if (state is WalletSingleCurrencyState) {
                    controlButtons(
                        configs = state.buttons,
                        modifier = movableItemModifier.padding(top = betweenItemsPadding),
                    )
                }

                notifications(configs = state.notifications, modifier = itemModifier)

                if (state is WalletSingleCurrencyState.Content) {
                    marketPriceBlock(state = state.marketPriceBlockState, modifier = itemModifier)
                }

                contentItems(state = state, txHistoryItems = txHistoryItems, modifier = movableItemModifier)

                if (state is WalletMultiCurrencyState) {
                    val tokensListState = state.tokensListState
                    if (tokensListState is WalletTokensListState.ContentState) {
                        organizeButton(onClick = tokensListState.onOrganizeTokensClick, modifier = itemModifier)
                    }
                }
            }

            WalletPullToRefreshIndicator(
                isRefreshing = state.pullToRefreshConfig.isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }

    WalletBottomSheets(state = state)

    WalletSideEffects(lazyListState = walletsListState, walletsListConfig = state.walletsListConfig)
}

@Composable
private fun BaseScaffold(state: WalletState.ContentState, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(
        topBar = { WalletTopBar(config = state.topBarConfig) },
        floatingActionButton = {
            if (state is WalletMultiCurrencyState.Content) {
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
    if (bottomSheetConfig != null && bottomSheetConfig.isShow) {
        WalletBottomSheet(config = bottomSheetConfig)
    }

    (state as? WalletMultiCurrencyState.Content)?.let { multiCurrencyState ->
        if (multiCurrencyState.tokenActionsBottomSheet != null && multiCurrencyState.tokenActionsBottomSheet.isShow) {
            TokenActionsBottomSheet(config = multiCurrencyState.tokenActionsBottomSheet)
        }
    }
}

// region Preview
@Preview
@Composable
private fun WalletScreenPreview_Light(@PreviewParameter(WalletScreenParameterProvider::class) state: WalletState) {
    TangemTheme {
        WalletScreen(state)
    }
}

@Preview
@Composable
private fun WalletScreenPreview_Dark(@PreviewParameter(WalletScreenParameterProvider::class) state: WalletState) {
    TangemTheme(isDark = true) {
        WalletScreen(state)
    }
}

private class WalletScreenParameterProvider : CollectionPreviewParameterProvider<WalletState>(
    collection = listOf(
        WalletPreviewData.multicurrencyWalletScreenState,
        WalletPreviewData.singleWalletScreenState,
    ),
)
// endregion Preview