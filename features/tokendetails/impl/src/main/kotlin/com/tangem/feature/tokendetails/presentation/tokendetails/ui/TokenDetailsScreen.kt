package com.tangem.feature.tokendetails.presentation.tokendetails.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.util.fastForEach
import androidx.paging.compose.collectAsLazyPagingItems
import com.tangem.core.ui.components.bottomsheets.chooseaddress.ChooseAddressBottomSheet
import com.tangem.core.ui.components.bottomsheets.chooseaddress.ChooseAddressBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.tokenreceive.TokenReceiveBottomSheet
import com.tangem.core.ui.components.bottomsheets.tokenreceive.TokenReceiveBottomSheetConfig
import com.tangem.core.ui.components.marketprice.MarketPriceBlock
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.components.transactions.Transaction
import com.tangem.core.ui.components.transactions.state.TransactionState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.components.transactions.txHistoryItems
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tokendetails.presentation.tokendetails.TokenDetailsPreviewData
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsNotification
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.TokenDetailsBalanceBlock
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.TokenDetailsDialogs
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.TokenDetailsTopAppBar
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.TokenInfoBlock
import kotlinx.collections.immutable.PersistentList

// TODO: Split to blocks [REDACTED_JIRA]
@Suppress("LongMethod")
@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun TokenDetailsScreen(state: TokenDetailsState) {
    BackHandler(onBack = state.topAppBarConfig.onBackClick)

    Scaffold(
        topBar = { TokenDetailsTopAppBar(config = state.topAppBarConfig) },
        containerColor = TangemTheme.colors.background.secondary,
    ) { scaffoldPaddings ->
        val pullRefreshState = rememberPullRefreshState(
            refreshing = state.pullToRefreshConfig.isRefreshing,
            onRefresh = state.pullToRefreshConfig.onRefresh,
        )

        val txHistoryItems = if (state.txHistoryState is TxHistoryState.Content) {
            state.txHistoryState.contentItems.collectAsLazyPagingItems()
        } else {
            null
        }
        val betweenItemsPadding = TangemTheme.dimens.spacing12
        val horizontalPadding = TangemTheme.dimens.spacing16
        val itemModifier = Modifier
            .padding(top = betweenItemsPadding)
            .padding(horizontal = horizontalPadding)

        Box(
            modifier = Modifier
                .padding(scaffoldPaddings)
                .pullRefresh(pullRefreshState),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
            ) {
                item {
                    TokenInfoBlock(
                        modifier = Modifier
                            .padding(top = TangemTheme.dimens.spacing4)
                            .padding(horizontal = horizontalPadding),
                        state = state.tokenInfoBlockState,
                    )
                }
                item {
                    TokenDetailsBalanceBlock(
                        modifier = itemModifier,
                        isBalanceHidden = state.isBalanceHidden,
                        state = state.tokenBalanceBlockState,
                    )
                }
                items(
                    items = state.notifications,
                    key = { it.config::class.java },
                    contentType = { it.config::class.java },
                    itemContent = {
                        Notification(
                            modifier = itemModifier.animateItemPlacement(),
                            config = it.config,
                            iconTint = when (it) {
                                is TokenDetailsNotification.Warning -> null
                                is TokenDetailsNotification.Informational -> TangemTheme.colors.icon.accent
                            },
                        )
                    },
                )
                if (!state.isCustomToken) {
                    item(
                        key = MarketPriceBlockState::class.java,
                        contentType = MarketPriceBlockState::class.java,
                        content = { MarketPriceBlock(modifier = itemModifier, state = state.marketPriceBlockState) },
                    )
                }
                if (state.txHistoryState is TxHistoryState.NotSupported && state.pendingTxs.isNotEmpty()) {
                    item {
                        PendingTxsBlock(
                            pendingTxs = state.pendingTxs,
                            isBalanceHidden = state.isBalanceHidden,
                            modifier = itemModifier,
                        )
                    }
                }
                txHistoryItems(
                    state = state.txHistoryState,
                    isBalanceHidden = state.isBalanceHidden,
                    txHistoryItems = txHistoryItems,
                )
            }

            PullRefreshIndicator(
                modifier = Modifier.align(Alignment.TopCenter),
                refreshing = state.pullToRefreshConfig.isRefreshing,
                state = pullRefreshState,
            )
        }

        TokenDetailsDialogs(state = state)

        state.bottomSheetConfig?.let { config ->
            if (config.isShow) {
                when (config.content) {
                    is TokenReceiveBottomSheetConfig -> {
                        TokenReceiveBottomSheet(config = config)
                    }
                    is ChooseAddressBottomSheetConfig -> {
                        ChooseAddressBottomSheet(config = config)
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingTxsBlock(
    pendingTxs: PersistentList<TransactionState>,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(shape = TangemTheme.shapes.roundedCornersXMedium)
            .background(color = TangemTheme.colors.background.primary),
        verticalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing8),
        horizontalAlignment = Alignment.Start,
    ) {
        pendingTxs.fastForEach { Transaction(state = it, isBalanceHidden = isBalanceHidden) }
    }
}

@Preview
@Composable
private fun Preview_TokenDetailsScreen_LightTheme() {
    TangemTheme(isDark = false) {
        TokenDetailsScreen(state = TokenDetailsPreviewData.tokenDetailsState)
    }
}

@Preview
@Composable
private fun Preview_TokenDetailsScreen_DarkTheme() {
    TangemTheme(isDark = true) {
        TokenDetailsScreen(state = TokenDetailsPreviewData.tokenDetailsState)
    }
}