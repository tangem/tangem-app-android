package com.tangem.feature.wallet.presentation.wallet.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.paging.compose.collectAsLazyPagingItems
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.wallet.state.WalletMultiCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletSingleCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletState
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
* [REDACTED_AUTHOR]
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

    Scaffold(
        topBar = { WalletTopBar(config = state.topBarConfig) },
        containerColor = TangemTheme.colors.background.secondary,
    ) { scaffoldPaddings ->

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
                state.txHistoryState is TxHistoryState.ContentState
            ) {
                (state.txHistoryState as? TxHistoryState.ContentState)?.items?.collectAsLazyPagingItems()
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
                contentPadding = PaddingValues(vertical = TangemTheme.dimens.spacing8),
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
                    organizeButton(onClick = state.tokensListState.onOrganizeTokensClick, modifier = itemModifier)
                }
            }

            WalletPullToRefreshIndicator(
                isRefreshing = state.pullToRefreshConfig.isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }

    val bottomSheetConfig = state.bottomSheetConfig
    if (bottomSheetConfig != null && bottomSheetConfig.isShow) {
        WalletBottomSheet(config = bottomSheetConfig)
    }

    WalletSideEffects(lazyListState = walletsListState, walletsListConfig = state.walletsListConfig)
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
