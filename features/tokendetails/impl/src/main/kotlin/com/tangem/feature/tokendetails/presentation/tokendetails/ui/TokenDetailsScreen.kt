package com.tangem.feature.tokendetails.presentation.tokendetails.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.common.ui.earn.EarnBlock
import com.tangem.common.ui.earn.EarnBlockUM
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateUM
import com.tangem.common.ui.expressStatus.state.ExpressTransactionsBlockState
import com.tangem.common.ui.notifications.notifications
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.containers.pullToRefresh.TangemPullToRefreshSlidingContainer
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownMenuItem
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.topFade
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.themedColor
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.test.TokenDetailsScreenTestTags
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.feature.tokendetails.presentation.tokendetails.state.*
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsTopAppBarUM.TitleState
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.TokenDetailsBalanceBlock
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.ZeroBalanceActionsBlock
import com.tangem.features.markets.token.block.TokenMarketBlockComponent
import com.tangem.features.tokendetails.ExpressTransactionsComponent
import com.tangem.features.txhistory.component.TxHistoryComponent
import com.tangem.features.txhistory.entity.TxHistoryItemsUM
import com.tangem.features.txhistory.entity.TxHistoryUM
import com.tangem.features.yield.supply.api.YieldSupplyComponent
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private val TopBarHeight: Dp = 64.dp
private val MarketBlockHorizontalPadding: Dp = 14.dp

private const val TOP_FADE_MID_STOP = 0.8f
private const val TOP_FADE_MID_ALPHA = 0.8f

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
    val hazeState = rememberHazeState()

    val rootBackground = TangemTheme.colors2.surface.level2
    var marketBlockHeight by remember { mutableStateOf(0.dp) }
    val effectiveBottomPadding = marketBlockHeight + TangemTheme.dimens2.x4

    CompositionLocalProvider(LocalHazeState provides hazeState) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(rootBackground),
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

            TokenDetailsTopBar(topAppBarUM = tokenDetailsUM.topAppBarUM)

            if (tokenMarketBlockComponent != null) {
                TokenDetailsMarketBlockOverlay(
                    component = tokenMarketBlockComponent,
                    onHeightChange = { marketBlockHeight = it },
                )
            }
            expressState.bottomSheetSlot?.content(null)
        }
    }
}

@Composable
private fun BoxScope.TokenDetailsMarketBlockOverlay(
    component: TokenMarketBlockComponent,
    onHeightChange: (Dp) -> Unit,
) {
    val density = LocalDensity.current

    TangemFade(
        variant = TangemFade.Variant.Hard,
        position = TangemFade.Position.Bottom,
        modifier = Modifier
            .fillMaxWidth()
            .height(174.dp)
            .align(Alignment.BottomCenter),
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

@Suppress("LongParameterList", "LongMethod")
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
        modifier = modifier
            .testTag(TokenDetailsScreenTestTags.SCREEN_CONTAINER)
            .hazeSourceTangem(state = LocalHazeState.current)
            .topFade(
                height = topContentPadding,
                0f to rootBackground,
                TOP_FADE_MID_STOP to rootBackground.copy(alpha = TOP_FADE_MID_ALPHA),
                1f to Color.Transparent,
            ),
        state = listState,
        contentPadding = PaddingValues(top = topContentPadding, bottom = bottomContentPadding),
    ) {
        item(key = "balance_block") {
            TokenDetailsBalanceBlock(
                balanceBlockUM = tokenDetailsUM.balanceBlockUM,
                isBalanceHidden = tokenDetailsUM.isBalanceHidden,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        val balance = tokenDetailsUM.balanceBlockUM
        notifications(
            notifications = tokenDetailsUM.notifications,
            contentColor = rootBackground,
            modifier = itemModifier,
        )
        tokenDetailsUM.earnBlockState?.let { earnBlock ->
            item(key = "staking_block") {
                val stakingTag = when {
                    earnBlock is EarnBlockUM.Content &&
                        earnBlock.type == EarnBlockUM.Type.Staking &&
                        earnBlock.trailingUM is EarnBlockUM.TrailingUM.Button ->
                        TokenDetailsScreenTestTags.STAKING_AVAILABLE_BLOCK
                    else -> TokenDetailsScreenTestTags.STAKING_BLOCK
                }
                EarnBlock(
                    state = earnBlock,
                    modifier = itemModifier
                        .padding(vertical = TangemTheme.dimens2.x3)
                        .testTag(stakingTag),
                )
            }
        }
        item(key = "yield_supply_block") {
            yieldSupplyComponent.Content(modifier = itemModifier.padding(vertical = TangemTheme.dimens2.x3))
        }
        if (balance is TokenDetailsBalanceBlockUM.Content && balance.isBalanceZero) {
            item(key = "zero_balance_actions") {
                ZeroBalanceActionsBlock(
                    state = tokenDetailsUM.zeroBalanceActionsUM,
                    modifier = itemModifier.padding(vertical = TangemTheme.dimens2.x2),
                )
            }
        }
        with(expressTransactionsComponent) {
            expressTransactionsContent(
                state = expressTransactionsToDisplay,
                modifier = expressTransactionModifier,
            )
        }
        tokenDetailsUM.quickTopUpBlock?.let { quickTopUpBlock ->
            item(key = "quick_top_up_block") {
                QuickTopUpBlock(
                    state = quickTopUpBlock,
                    modifier = itemModifier.padding(vertical = TangemTheme.dimens2.x0),
                )
            }
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