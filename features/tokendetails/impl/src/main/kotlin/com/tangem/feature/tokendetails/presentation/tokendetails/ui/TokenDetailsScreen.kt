package com.tangem.feature.tokendetails.presentation.tokendetails.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.paging.compose.collectAsLazyPagingItems
import com.tangem.core.ui.components.bottomsheets.chooseaddress.ChooseAddressBottomSheet
import com.tangem.core.ui.components.bottomsheets.chooseaddress.ChooseAddressBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.tokenreceive.TokenReceiveBottomSheet
import com.tangem.core.ui.components.bottomsheets.tokenreceive.TokenReceiveBottomSheetConfig
import com.tangem.core.ui.components.marketprice.MarketPriceBlock
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.components.notifications.OkxPromoNotification
import com.tangem.core.ui.components.snackbar.TangemSnackbarHost
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.components.transactions.txHistoryItems
import com.tangem.core.ui.event.EventEffect
import com.tangem.core.ui.event.StateEvent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.tokendetails.presentation.tokendetails.TokenDetailsPreviewData
import com.tangem.feature.tokendetails.presentation.tokendetails.state.StakingBlockUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsNotification
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.TokenDetailsBalanceBlock
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.TokenDetailsDialogs
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.TokenDetailsTopAppBar
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.TokenInfoBlock
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.express.ExpressStatusBottomSheet
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.express.ExpressStatusBottomSheetConfig
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.express.expressTransactionsItems
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.staking.TokenStakingBlock
import com.tangem.features.markets.token.block.TokenMarketBlockComponent

// TODO: Split to blocks [REDACTED_JIRA]
@Suppress("LongMethod")
@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun TokenDetailsScreen(state: TokenDetailsState, tokenMarketBlockComponent: TokenMarketBlockComponent?) {
    BackHandler(onBack = state.topAppBarConfig.onBackClick)
    val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }

    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        topBar = { TokenDetailsTopAppBar(config = state.topAppBarConfig) },
        snackbarHost = {
            TangemSnackbarHost(
                modifier = Modifier.padding(
                    start = TangemTheme.dimens.spacing16,
                    end = TangemTheme.dimens.spacing16,
                    bottom = bottomBarHeight + TangemTheme.dimens.spacing16,
                ),
                hostState = snackbarHostState,
            )
        },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.navigationBars),
        containerColor = TangemTheme.colors.background.secondary,
    ) { scaffoldPaddings ->
        val pullRefreshState = rememberPullRefreshState(
            refreshing = state.pullToRefreshConfig.isRefreshing,
            onRefresh = { state.pullToRefreshConfig.onRefresh(PullToRefreshConfig.ShowRefreshState()) },
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
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    bottom = TangemTheme.dimens.spacing16 + bottomBarHeight,
                ),
            ) {
                item {
                    TokenInfoBlock(
                        modifier = Modifier.padding(horizontal = horizontalPadding),
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
                    key = { it::class.java },
                    contentType = { it.config::class.java },
                    itemContent = {
                        if (it is TokenDetailsNotification.SwapPromo) {
                            OkxPromoNotification(
                                config = it.config,
                                modifier = itemModifier.animateItem(),
                            )
                        } else {
                            Notification(
                                modifier = itemModifier.animateItem(),
                                config = it.config,
                                iconTint = when (it) {
                                    is TokenDetailsNotification.Informational -> TangemTheme.colors.icon.accent
                                    else -> null
                                },
                            )
                        }
                    },
                )

                when {
                    tokenMarketBlockComponent != null -> {
                        item(
                            key = TokenMarketBlockComponent::class.java,
                            contentType = TokenMarketBlockComponent::class.java,
                            content = { tokenMarketBlockComponent.Content(modifier = itemModifier) },
                        )
                    }
                    state.isMarketPriceAvailable -> {
                        item(
                            key = MarketPriceBlockState::class.java,
                            contentType = MarketPriceBlockState::class.java,
                            content = {
                                MarketPriceBlock(
                                    modifier = itemModifier,
                                    state = state.marketPriceBlockState,
                                )
                            },
                        )
                    }
                }

                if (state.stakingBlocksState != null) {
                    item(
                        key = StakingBlockUM::class.java,
                        contentType = StakingBlockUM::class.java,
                        content = {
                            TokenStakingBlock(
                                state = state.stakingBlocksState,
                                isBalanceHidden = state.isBalanceHidden,
                                modifier = itemModifier,
                            )
                        },
                    )
                }

                expressTransactionsItems(
                    expressTxs = state.expressTxs,
                    modifier = itemModifier,
                )

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
            when (config.content) {
                is TokenReceiveBottomSheetConfig -> {
                    TokenReceiveBottomSheet(config = config)
                }
                is ChooseAddressBottomSheetConfig -> {
                    ChooseAddressBottomSheet(config = config)
                }
                is ExpressStatusBottomSheetConfig -> {
                    ExpressStatusBottomSheet(config = config)
                }
            }
        }

        TokenDetailsEventEffect(snackbarHostState = snackbarHostState, event = state.event)
    }
}

@Composable
internal fun TokenDetailsEventEffect(snackbarHostState: SnackbarHostState, event: StateEvent<TextReference>) {
    val resources = LocalContext.current.resources

    EventEffect(
        event = event,
        onTrigger = { value ->
            snackbarHostState.showSnackbar(message = value.resolveReference(resources))
        },
    )
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TokenDetailsScreenPreview(
    @PreviewParameter(TokenDetailsScreenParameterProvider::class) state: TokenDetailsState,
) {
    TangemThemePreview {
        TokenDetailsScreen(
            state = state,
            tokenMarketBlockComponent = null,
        )
    }
}

private class TokenDetailsScreenParameterProvider : CollectionPreviewParameterProvider<TokenDetailsState>(
    collection = listOf(
        TokenDetailsPreviewData.tokenDetailsState_1,
        TokenDetailsPreviewData.tokenDetailsState_2,
        TokenDetailsPreviewData.tokenDetailsState_3,
    ),
)
// endregion Preview