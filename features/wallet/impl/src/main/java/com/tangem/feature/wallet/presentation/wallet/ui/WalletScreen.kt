package com.tangem.feature.wallet.presentation.wallet.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.buttons.HorizontalActionChips
import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.components.buttons.actions.RoundedActionButton
import com.tangem.core.ui.components.marketprice.MarketPriceBlock
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.components.transactions.Transaction
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.common.component.NetworkGroupItem
import com.tangem.feature.wallet.presentation.common.component.TokenItem
import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import com.tangem.feature.wallet.presentation.wallet.state.WalletContentItemState
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateHolder
import com.tangem.feature.wallet.presentation.wallet.ui.components.WalletBottomSheet
import com.tangem.feature.wallet.presentation.wallet.ui.components.WalletTopBar
import com.tangem.feature.wallet.presentation.wallet.ui.components.WalletsList
import com.tangem.feature.wallet.presentation.wallet.ui.components.singlecurrency.TransactionsBlockGroupTitle
import com.tangem.feature.wallet.presentation.wallet.ui.components.singlecurrency.TransactionsBlockTitle
import com.tangem.feature.wallet.presentation.wallet.ui.decorations.walletContentItemDecoration
import com.tangem.feature.wallet.presentation.wallet.ui.utils.changeWalletAnimator

/**
 * Wallet screen
 *
 * @param state screen state
 *
[REDACTED_AUTHOR]
 */
@OptIn(ExperimentalMaterialApi::class)
@Suppress("LongMethod")
@Composable
internal fun WalletScreen(state: WalletStateHolder) {
    BackHandler(onBack = state.onBackClick)

    Scaffold(
        topBar = { WalletTopBar(config = state.topBarConfig) },
        containerColor = TangemTheme.colors.background.secondary,
    ) { scaffoldPaddings ->

        val walletsListState = rememberLazyListState()
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
                            modifier = changeableItemModifier
                                .padding(top = TangemTheme.dimens.spacing14),
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

                itemsIndexed(
                    items = state.contentItems,
                    key = { index, item ->
                        when (item) {
                            is WalletContentItemState.MultiCurrencyItem.NetworkGroupTitle -> item.networkName
                            is WalletContentItemState.MultiCurrencyItem.Token -> index
                            is WalletContentItemState.SingleCurrencyItem.Title -> index
                            is WalletContentItemState.SingleCurrencyItem.GroupTitle -> item.title
                            is WalletContentItemState.SingleCurrencyItem.Transaction -> index
                            is WalletContentItemState.Loading -> index
                        }
                    },
                    itemContent = { index, item ->
                        ContentItem(
                            item = item,
                            modifier = changeableItemModifier.walletContentItemDecoration(
                                currentIndex = index,
                                lastIndex = state.contentItems.lastIndex,
                            ),
                        )
                    },
                )

                if (state is WalletStateHolder.MultiCurrencyContent) {
                    item {
                        OrganizeTokensButton(
                            onClick = state.onOrganizeTokensClick,
                            modifier = changeableItemModifier
                                .padding(top = TangemTheme.dimens.spacing14)
                                .padding(horizontal = TangemTheme.dimens.spacing16),
                        )
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = state.pullToRefreshConfig.isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }

    state.bottomSheet?.let { bottomSheetConfig ->
        if (bottomSheetConfig.isShow) WalletBottomSheet(config = bottomSheetConfig)
    }
}

@Composable
private fun ContentItem(item: WalletContentItemState, modifier: Modifier = Modifier) {
    when (item) {
        is WalletContentItemState.MultiCurrencyItem.NetworkGroupTitle -> {
            NetworkGroupItem(networkName = item.networkName, modifier = modifier)
        }
        is WalletContentItemState.MultiCurrencyItem.Token -> {
            TokenItem(state = item.state, modifier = modifier)
        }
        is WalletContentItemState.SingleCurrencyItem.Title -> {
            TransactionsBlockTitle(config = item, modifier = modifier)
        }
        is WalletContentItemState.SingleCurrencyItem.GroupTitle -> {
            TransactionsBlockGroupTitle(config = item, modifier = modifier)
        }
        is WalletContentItemState.SingleCurrencyItem.Transaction -> {
            Transaction(state = item.state, modifier = modifier)
        }
        WalletContentItemState.Loading -> {
            TokenItem(state = TokenItemState.Loading, modifier = modifier)
        }
    }
}

@Composable
private fun OrganizeTokensButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    RoundedActionButton(
        config = ActionButtonConfig(
            text = stringResource(id = R.string.organize_tokens_title),
            iconResId = R.drawable.ic_filter_24,
            onClick = onClick,
        ),
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