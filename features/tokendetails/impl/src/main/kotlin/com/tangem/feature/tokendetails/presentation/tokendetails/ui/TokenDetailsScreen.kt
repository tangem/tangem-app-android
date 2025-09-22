package com.tangem.feature.tokendetails.presentation.tokendetails.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.common.ui.bottomsheet.chooseaddress.ChooseAddressBottomSheet
import com.tangem.common.ui.bottomsheet.chooseaddress.ChooseAddressBottomSheetConfig
import com.tangem.common.ui.bottomsheet.receive.TokenReceiveBottomSheet
import com.tangem.common.ui.bottomsheet.receive.TokenReceiveBottomSheetConfig
import com.tangem.common.ui.expressStatus.ExpressStatusBottomSheetConfig
import com.tangem.common.ui.expressStatus.expressTransactionsItems
import com.tangem.core.ui.components.containers.pullToRefresh.TangemPullToRefreshContainer
import com.tangem.core.ui.components.marketprice.MarketPriceBlock
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.TokenDetailsScreenTestTags
import com.tangem.feature.tokendetails.presentation.tokendetails.TokenDetailsPreviewData
import com.tangem.feature.tokendetails.presentation.tokendetails.state.StakingBlockUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsNotification
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.TokenDetailsBalanceBlock
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.TokenDetailsDialogs
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.TokenDetailsTopAppBar
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.TokenInfoBlock
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.express.ExpressStatusBottomSheet
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.staking.TokenStakingBlock
import com.tangem.features.markets.token.block.TokenMarketBlockComponent
import com.tangem.features.txhistory.component.TxHistoryComponent
import com.tangem.features.txhistory.entity.TxHistoryUM
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// TODO: Split to blocks [REDACTED_JIRA]
@Suppress("LongMethod")
@Composable
internal fun TokenDetailsScreen(
    state: TokenDetailsState,
    tokenMarketBlockComponent: TokenMarketBlockComponent?,
    txHistoryComponent: TxHistoryComponent,
) {
    val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }

    Scaffold(
        topBar = { TokenDetailsTopAppBar(config = state.topAppBarConfig) },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.navigationBars),
        containerColor = TangemTheme.colors.background.secondary,
    ) { scaffoldPaddings ->
        val listState = rememberLazyListState()
        val txHistoryComponentState by txHistoryComponent.txHistoryState.collectAsStateWithLifecycle()
        val betweenItemsPadding = TangemTheme.dimens.spacing12
        val horizontalPadding = TangemTheme.dimens.spacing16
        val itemModifier = Modifier
            .padding(top = betweenItemsPadding)
            .padding(horizontal = horizontalPadding)

        TangemPullToRefreshContainer(
            config = state.pullToRefreshConfig,
            modifier = Modifier.padding(scaffoldPaddings),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(TokenDetailsScreenTestTags.SCREEN_CONTAINER),
                state = listState,
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
                        Notification(
                            modifier = itemModifier.animateItem(),
                            config = it.config,
                            iconTint = when (it) {
                                is TokenDetailsNotification.Informational -> TangemTheme.colors.icon.accent
                                is TokenDetailsNotification.UsedOutdatedData -> TangemTheme.colors.text.attention
                                else -> null
                            },
                        )
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
                    expressTxs = state.expressTxsToDisplay,
                    modifier = itemModifier,
                )

                with(txHistoryComponent) { txHistoryContent(listState = listState, state = txHistoryComponentState) }
            }
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
    }
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
            txHistoryComponent = object : TxHistoryComponent {
                override val txHistoryState: StateFlow<TxHistoryUM> = MutableStateFlow(
                    value = TxHistoryUM.Empty(isBalanceHidden = false, onExploreClick = {}),
                )

                override fun LazyListScope.txHistoryContent(listState: LazyListState, state: TxHistoryUM) = Unit
            },
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