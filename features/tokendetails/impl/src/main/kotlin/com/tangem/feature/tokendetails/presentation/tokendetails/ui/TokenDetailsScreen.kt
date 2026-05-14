package com.tangem.feature.tokendetails.presentation.tokendetails.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import com.tangem.common.ui.earn.EarnBlock
import com.tangem.common.ui.notifications.notifications
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateUM
import com.tangem.common.ui.expressStatus.state.ExpressTransactionsBlockState
import com.tangem.core.ui.components.BottomFade
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.components.containers.pullToRefresh.TangemPullToRefreshSlidingContainer
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownMenuItem
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.themedColor
import com.tangem.core.ui.res.LocalRootBackgroundColor
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.feature.tokendetails.presentation.tokendetails.state.AddFundsUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenBalanceTypeUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TransferUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsBalanceBlockUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsTopAppBarUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsTopAppBarUM.TitleState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.ZeroBalanceActionsUM
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.TokenDetailsBalanceBlock
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.ZeroBalanceActionsBlock
import com.tangem.features.markets.token.block.TokenMarketBlockComponent
import com.tangem.features.tokendetails.ExpressTransactionsComponent
import com.tangem.features.txhistory.component.TxHistoryComponent
import com.tangem.features.txhistory.entity.TxHistoryItemsUM
import com.tangem.features.txhistory.entity.TxHistoryUM
import com.tangem.features.yield.supply.api.YieldSupplyComponent
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private val TopBarHeight: Dp = 64.dp
private val MarketBlockHorizontalPadding: Dp = 14.dp

@Composable
internal fun TokenDetailsScreen(
    tokenDetailsUM: TokenDetailsUM,
    tokenMarketBlockComponent: TokenMarketBlockComponent?,
    yieldSupplyComponent: YieldSupplyComponent,
    txHistoryComponent: TxHistoryComponent,
    expressTransactionsComponent: ExpressTransactionsComponent,
    modifier: Modifier = Modifier,
) {
    val expressState by expressTransactionsComponent.state.collectAsStateWithLifecycle()
    val statusBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getTop(this).toDp() }
    val topBarTotalHeight = TopBarHeight + statusBarHeight

    val rootBackground by LocalRootBackgroundColor.current
    var marketBlockHeight by remember { mutableStateOf(0.dp) }
    val effectiveBottomPadding = marketBlockHeight + TangemTheme.dimens2.x4

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors2.surface.level2),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSourceTangem(zIndex = -2f),
        ) {
            TangemPullToRefreshSlidingContainer(
                config = tokenDetailsUM.pullToRefreshConfig,
                indicatorOffset = topBarTotalHeight,
            ) {
                TokenDetailsBody(
                    tokenDetailsUM = tokenDetailsUM,
                    yieldSupplyComponent = yieldSupplyComponent,
                    txHistoryComponent = txHistoryComponent,
                    expressTransactionsComponent = expressTransactionsComponent,
                    expressTransactionsToDisplay = expressState.transactionsToDisplay,
                    rootBackground = rootBackground,
                    topContentPadding = topBarTotalHeight,
                    bottomContentPadding = effectiveBottomPadding,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        TokenDetailsTopBarOverlay(topAppBarUM = tokenDetailsUM.topAppBarUM)

        if (tokenMarketBlockComponent != null) {
            TokenDetailsMarketBlockOverlay(
                component = tokenMarketBlockComponent,
                onHeightChange = { marketBlockHeight = it },
            )
        }

        expressState.bottomSheetSlot?.content()
    }
}

@Composable
private fun TokenDetailsTopBarOverlay(topAppBarUM: TokenDetailsTopAppBarUM) {
    Box(
        modifier = Modifier.background(TangemTheme.colors2.surface.level2),
    ) {
        TokenDetailsTopBar(topAppBarUM = topAppBarUM)
    }
}

@Composable
private fun BoxScope.TokenDetailsMarketBlockOverlay(
    component: TokenMarketBlockComponent,
    onHeightChange: (Dp) -> Unit,
) {
    val density = LocalDensity.current

    BottomFade(
        backgroundColor = TangemTheme.colors2.surface.level2,
        modifier = Modifier.align(Alignment.BottomCenter),
    )

    component.Content(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .onSizeChanged { size ->
                onHeightChange(with(density) { size.height.toDp() })
            }
            .navigationBarsPadding()
            .padding(
                horizontal = MarketBlockHorizontalPadding,
                vertical = TangemTheme.dimens2.x1_5,
            ),
    )
}

@Suppress("LongParameterList")
@Composable
private fun TokenDetailsBody(
    tokenDetailsUM: TokenDetailsUM,
    yieldSupplyComponent: YieldSupplyComponent,
    txHistoryComponent: TxHistoryComponent,
    expressTransactionsComponent: ExpressTransactionsComponent,
    expressTransactionsToDisplay: PersistentList<ExpressTransactionStateUM>,
    rootBackground: Color,
    topContentPadding: Dp,
    bottomContentPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val txHistoryState by txHistoryComponent.txHistoryState.collectAsStateWithLifecycle()
    val itemModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = TangemTheme.dimens2.x4)

    val expressTransactionModifier = Modifier
        .fillMaxWidth()
        .padding(start = TangemTheme.dimens2.x4, end = TangemTheme.dimens2.x4, top = TangemTheme.dimens2.x4)

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(top = topContentPadding, bottom = bottomContentPadding),
    ) {
        item(key = "balance_block") {
            TokenDetailsBalanceBlock(
                balanceBlockUM = tokenDetailsUM.balanceBlockUM,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        val balance = tokenDetailsUM.balanceBlockUM
        if (balance is TokenDetailsBalanceBlockUM.Content && balance.isBalanceZero) {
            item(key = "zero_balance_actions") {
                ZeroBalanceActionsBlock(
                    state = tokenDetailsUM.zeroBalanceActionsUM,
                    modifier = itemModifier,
                )
            }
        }
        notifications(
            notifications = tokenDetailsUM.notifications,
            contentColor = rootBackground,
            modifier = itemModifier,
        )
        tokenDetailsUM.earnBlockState?.let { earnBlock ->
            item(key = "staking_block") {
                EarnBlock(
                    state = earnBlock,
                    modifier = itemModifier,
                )
            }
        }
        item(key = "yield_supply_block") {
            yieldSupplyComponent.Content(modifier = itemModifier.padding(vertical = TangemTheme.dimens2.x2))
        }
        with(expressTransactionsComponent) {
            expressTransactionsContent(
                state = expressTransactionsToDisplay,
                modifier = expressTransactionModifier,
            )
        }
        with(txHistoryComponent) {
            txHistoryContent(listState = listState, state = txHistoryState)
        }
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TokenDetailsScreen_Preview() {
    TangemThemePreviewRedesign {
        TokenDetailsScreen(
            tokenMarketBlockComponent = null,
            tokenDetailsUM = TokenDetailsUM(
                topAppBarUM = TokenDetailsTopAppBarUM(
                    titleState = TitleState.WithAccount(
                        tokenName = "Tether",
                        accountName = stringReference("Portfolio"),
                        accountIconUM = AccountIconUM.CryptoPortfolio(
                            value = CryptoPortfolioIcon.Icon.Star,
                            color = CryptoPortfolioIcon.Color.Azure,
                        ),
                    ),
                    subtitle = stringReference("ERC-20 in Ethereum network"),
                    onBackClick = {},
                    menuItems = persistentListOf(
                        TangemDropdownMenuItem(
                            title = stringReference("Hide Token"),
                            textColor = themedColor { TangemTheme.colors.text.warning },
                            onClick = {},
                        ),
                    ),
                ),
                notifications = persistentListOf(),
                earnBlockState = null,
                balanceBlockUM = TokenDetailsBalanceBlockUM.Loading(
                    addFundsButton = previewActionButton(),
                    swapButton = previewActionButton(),
                    transferButton = previewActionButton(),
                    tokenBalanceTypeUM = TokenBalanceTypeUM.Single,
                    currencyIconState = CurrencyIconState.Loading,
                ),
                marketPriceBlockState = MarketPriceBlockState.Loading(currencySymbol = "USDT"),
                pullToRefreshConfig = PullToRefreshConfig(
                    isRefreshing = false,
                    onRefresh = {},
                ),
                isBalanceHidden = false,
                isMarketPriceAvailable = true,
                addFundsUM = AddFundsUM.Loading,
                transferUM = TransferUM.Loading,
                zeroBalanceActionsUM = ZeroBalanceActionsUM.Loading,
            ),
            yieldSupplyComponent = object : YieldSupplyComponent {
                @Composable
                override fun Content(modifier: Modifier) = Unit
            },
            txHistoryComponent = object : TxHistoryComponent {
                override val legacyTxHistoryState: StateFlow<TxHistoryUM> = MutableStateFlow(
                    value = TxHistoryUM.Empty(isBalanceHidden = false, onExploreClick = {}),
                )

                override val txHistoryState: StateFlow<TxHistoryItemsUM> = MutableStateFlow(
                    value = TxHistoryItemsUM.Empty(isBalanceHidden = false, onExploreClick = {}),
                )

                override fun LazyListScope.txHistoryContentLegacy(listState: LazyListState, state: TxHistoryUM) = Unit

                override fun LazyListScope.txHistoryContent(listState: LazyListState, state: TxHistoryItemsUM) = Unit
            },
            expressTransactionsComponent = PreviewExpressTransactionsComponent,
        )
    }
}

private fun previewActionButton(): TangemButtonUM = TangemButtonUM(
    text = stringReference(""),
    onClick = { },
    isEnabled = true,
    type = TangemButtonType.Secondary,
)

private val PreviewExpressTransactionsComponent = object : ExpressTransactionsComponent {
    override val state: StateFlow<ExpressTransactionsBlockState> = MutableStateFlow(
        ExpressTransactionsBlockState(
            transactions = persistentListOf(),
            transactionsToDisplay = persistentListOf(),
            bottomSheetSlot = null,
        ),
    )

    override fun LazyListScope.expressTransactionsContentLegacy(
        state: PersistentList<ExpressTransactionStateUM>,
        modifier: Modifier,
    ) = Unit

    override fun LazyListScope.expressTransactionsContent(
        state: PersistentList<ExpressTransactionStateUM>,
        modifier: Modifier,
    ) = Unit
}
// endregion Preview