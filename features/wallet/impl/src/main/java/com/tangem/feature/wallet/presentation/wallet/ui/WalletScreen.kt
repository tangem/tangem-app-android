package com.tangem.feature.wallet.presentation.wallet.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemsIndexed
import com.tangem.core.ui.components.buttons.HorizontalActionChips
import com.tangem.core.ui.components.marketprice.MarketPriceBlock
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateHolder
import com.tangem.feature.wallet.presentation.wallet.state.content.WalletTokensListState
import com.tangem.feature.wallet.presentation.wallet.state.content.WalletTxHistoryState
import com.tangem.feature.wallet.presentation.wallet.ui.components.WalletBottomSheet
import com.tangem.feature.wallet.presentation.wallet.ui.components.WalletTopBar
import com.tangem.feature.wallet.presentation.wallet.ui.components.WalletsList
import com.tangem.feature.wallet.presentation.wallet.ui.components.multicurrency.MultiCurrencyContentItem
import com.tangem.feature.wallet.presentation.wallet.ui.components.multicurrency.OrganizeTokensButton
import com.tangem.feature.wallet.presentation.wallet.ui.components.singlecurrency.SingleCurrencyContentItem
import com.tangem.feature.wallet.presentation.wallet.ui.decorations.walletContentItemDecoration
import com.tangem.feature.wallet.presentation.wallet.ui.utils.ScrollOffsetCollector
import com.tangem.feature.wallet.presentation.wallet.ui.utils.changeWalletAnimator

/**
 * Wallet screen
 *
 * @param state screen state
 *
* [REDACTED_AUTHOR]
 */
@OptIn(ExperimentalMaterialApi::class)
@Suppress("LongMethod")
@Composable
internal fun WalletScreen(state: WalletStateHolder) {
    BackHandler(onBack = state.onBackClick)
    val walletsListState = rememberLazyListState()

    Scaffold(
        topBar = { WalletTopBar(config = state.topBarConfig) },
        containerColor = TangemTheme.colors.background.secondary,
    ) { scaffoldPaddings ->

        val changeableItemModifier = Modifier.changeWalletAnimator(walletsListState)
        val pullRefreshState = rememberPullRefreshState(
            refreshing = state.pullToRefreshConfig.isRefreshing,
            onRefresh = state.pullToRefreshConfig.onRefresh,
        )

        Box(
            modifier = Modifier
                .padding(paddingValues = scaffoldPaddings)
                .pullRefresh(pullRefreshState),
        ) {
            val txHistoryItems = if (state is WalletStateHolder.SingleCurrencyContent) {
                if (state.txHistoryState is WalletTxHistoryState.ContentItemsState) {
                    state.txHistoryState.items.collectAsLazyPagingItems()
                } else {
                    null
                }
            } else {
                null
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = TangemTheme.dimens.spacing8),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item {
                    WalletsList(
                        config = state.walletsListConfig,
                        lazyListState = walletsListState,
                    )
                }

                if (state is WalletStateHolder.SingleCurrencyContent) {
                    item {
                        HorizontalActionChips(
                            buttons = state.buttons,
                            modifier = changeableItemModifier.padding(top = TangemTheme.dimens.spacing14),
                            contentPadding = PaddingValues(horizontal = TangemTheme.dimens.spacing16),
                        )
                    }
                }

                items(
                    items = state.notifications,
                    itemContent = { item ->
                        Notification(
                            state = item.state,
                            modifier = changeableItemModifier
                                .padding(top = TangemTheme.dimens.spacing14)
                                .padding(horizontal = TangemTheme.dimens.spacing16),
                        )
                    },
                )

                if (state is WalletStateHolder.SingleCurrencyContent) {
                    item {
                        MarketPriceBlock(
                            state = state.marketPriceBlockState,
                            modifier = changeableItemModifier
                                .padding(top = TangemTheme.dimens.spacing14)
                                .padding(horizontal = TangemTheme.dimens.spacing16),
                        )
                    }
                }

                contentItems(state = state, txHistoryItems = txHistoryItems, modifier = changeableItemModifier)

                if (state is WalletStateHolder.MultiCurrencyContent) {
                    item {
                        OrganizeTokensButton(
                            onClick = state.tokensListState.onOrganizeTokensClick,
                            modifier = changeableItemModifier
                                .padding(top = TangemTheme.dimens.spacing14)
                                .padding(horizontal = TangemTheme.dimens.spacing16),
                        )
                    }
                }
            }

            PullToRefreshIndicator(
                isRefreshing = state.pullToRefreshConfig.isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }

    state.bottomSheet?.let { bottomSheetConfig ->
        if (bottomSheetConfig.isShow) WalletBottomSheet(config = bottomSheetConfig)
    }

    LaunchedEffect(key1 = walletsListState, key2 = state.walletsListConfig.onWalletChange) {
        snapshotFlow { walletsListState.layoutInfo.visibleItemsInfo }
            .collect(collector = ScrollOffsetCollector(callback = state.walletsListConfig.onWalletChange))
    }
}

private fun LazyListScope.contentItems(
    state: WalletStateHolder,
    txHistoryItems: LazyPagingItems<WalletTxHistoryState.TxHistoryItemState>?,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is WalletStateHolder.MultiCurrencyContent -> {
            tokensListItems(state = state.tokensListState, modifier = modifier)
        }
        is WalletStateHolder.SingleCurrencyContent -> {
            txHistoryItems(
                state = state.txHistoryState,
                txHistoryItems = txHistoryItems,
                modifier = modifier,
            )
        }
        is WalletStateHolder.Loading,
        is WalletStateHolder.UnlockWalletContent,
        -> Unit
    }
}

private fun LazyListScope.tokensListItems(state: WalletTokensListState, modifier: Modifier = Modifier) {
    itemsIndexed(
        items = state.items,
        key = { index, _ -> index },
        itemContent = { index, item ->
            MultiCurrencyContentItem(
                state = item,
                modifier = modifier.walletContentItemDecoration(
                    currentIndex = index,
                    lastIndex = state.items.lastIndex,
                ),
            )
        },
    )
}

private fun LazyListScope.txHistoryItems(
    state: WalletTxHistoryState,
    txHistoryItems: LazyPagingItems<WalletTxHistoryState.TxHistoryItemState>?,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is WalletTxHistoryState.ContentItemsState -> {
            checkNotNull(txHistoryItems)
            itemsIndexed(
                items = txHistoryItems,
                key = { index, _ -> index },
                itemContent = { index, item ->
                    if (item == null) return@itemsIndexed

                    SingleCurrencyContentItem(
                        state = item,
                        modifier = modifier.walletContentItemDecoration(
                            currentIndex = index,
                            lastIndex = txHistoryItems.itemCount,
                        ),
                    )
                },
            )
        }
        is WalletTxHistoryState.Empty -> TODO("https://tangem.atlassian.net/browse/AND-4133")
        is WalletTxHistoryState.Error -> TODO("https://tangem.atlassian.net/browse/AND-4133")
        is WalletTxHistoryState.NotSupported -> TODO("https://tangem.atlassian.net/browse/AND-4133")
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun PullToRefreshIndicator(isRefreshing: Boolean, state: PullRefreshState, modifier: Modifier = Modifier) {
    PullRefreshIndicator(
        refreshing = isRefreshing,
        state = state,
        modifier = modifier,
    )
}

// region Preview
@Preview
@Composable
private fun WalletScreenPreview_Light(
    @PreviewParameter(WalletScreenParameterProvider::class) state: WalletStateHolder,
) {
    TangemTheme {
        WalletScreen(state)
    }
}

@Preview
@Composable
private fun WalletScreenPreview_Dark(
    @PreviewParameter(WalletScreenParameterProvider::class) state: WalletStateHolder,
) {
    TangemTheme(isDark = true) {
        WalletScreen(state)
    }
}

private class WalletScreenParameterProvider : CollectionPreviewParameterProvider<WalletStateHolder>(
    collection = listOf(
        WalletPreviewData.multicurrencyWalletScreenState,
        WalletPreviewData.singleWalletScreenState,
    ),
)
// endregion Preview
