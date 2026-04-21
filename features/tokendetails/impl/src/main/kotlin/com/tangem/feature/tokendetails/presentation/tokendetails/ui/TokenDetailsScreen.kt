package com.tangem.feature.tokendetails.presentation.tokendetails.ui

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import com.tangem.common.ui.earn.EarnBlock
import com.tangem.common.ui.notifications.notifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.components.BottomFade
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownMenuItem
import com.tangem.core.ui.components.haze.hazeEffectTangem
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.ds.topbar.collapsing.TangemCollapsingTopBar
import com.tangem.core.ui.ds.topbar.collapsing.rememberTangemExitUntilCollapsedScrollBehavior
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.themedColor
import com.tangem.core.ui.res.LocalRootBackgroundColor
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenBalanceTypeUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsBalanceBlockUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsTopAppBarUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsTopAppBarUM.TitleState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsUM
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.TokenDetailsBalanceBlock
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.TokenDetailsBalanceBlockHeight
import com.tangem.features.markets.token.block.TokenMarketBlockComponent
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeTint
import kotlinx.collections.immutable.persistentListOf

private val TopBarHeight: Dp = 64.dp
private val MarketBlockHorizontalPadding: Dp = 14.dp

@Composable
internal fun TokenDetailsScreen(
    tokenDetailsUM: TokenDetailsUM,
    tokenMarketBlockComponent: TokenMarketBlockComponent?,
    modifier: Modifier = Modifier,
) {
    val statusBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getTop(this).toDp() }
    val partialCollapsedHeight = TopBarHeight + statusBarHeight
    val expandedHeight = TokenDetailsBalanceBlockHeight + partialCollapsedHeight

    val behavior = rememberTangemExitUntilCollapsedScrollBehavior(
        expandedHeight = expandedHeight,
        partialCollapsedHeight = partialCollapsedHeight,
    )

    val rootBackground by LocalRootBackgroundColor.current
    var marketBlockHeight by remember { mutableStateOf(0.dp) }
    val notificationModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = TangemTheme.dimens2.x4)

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSourceTangem(zIndex = -2f),
        ) {
            TangemCollapsingTopBar(
                state = behavior.state,
                collapsingPart = {
                    TokenDetailsBalanceBlock(
                        balanceBlockUM = tokenDetailsUM.balanceBlockUM,
                        behavior = behavior,
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(top = TopBarHeight),
                    )
                },
                body = {
                    TokenDetailsBody(
                        tokenDetailsUM = tokenDetailsUM,
                        rootBackground = rootBackground,
                        bottomContentPadding = marketBlockHeight,
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(behavior.nestedScrollConnection),
                        itemModifier = notificationModifier,
                    )
                },
            )
        }

        TokenDetailsTopBarOverlay(
            topAppBarUM = tokenDetailsUM.topAppBarUM,
            collapsedFraction = behavior.state.collapsedFraction,
            rootBackground = rootBackground,
        )

        if (tokenMarketBlockComponent != null) {
            TokenDetailsMarketBlockOverlay(
                component = tokenMarketBlockComponent,
                rootBackground = rootBackground,
                onHeightChange = { marketBlockHeight = it },
            )
        }
    }
}

@Composable
private fun TokenDetailsTopBarOverlay(
    topAppBarUM: TokenDetailsTopAppBarUM,
    collapsedFraction: Float,
    rootBackground: Color,
) {
    val hazeIntensity by animateFloatAsState(
        targetValue = (collapsedFraction * 2f).coerceIn(0f, 1f),
        label = "TopBarHazeIntensity",
    )
    Box(
        modifier = Modifier.hazeEffectTangem {
            fallbackTint = HazeTint(rootBackground.copy(alpha = hazeIntensity / 2f))
            progressive = HazeProgressive.verticalGradient(
                startIntensity = hazeIntensity,
                endIntensity = 0f,
                preferPerformance = true,
            )
        },
    ) {
        TokenDetailsTopBar(topAppBarUM = topAppBarUM)
    }
}

@Composable
private fun BoxScope.TokenDetailsMarketBlockOverlay(
    component: TokenMarketBlockComponent,
    rootBackground: Color,
    onHeightChange: (Dp) -> Unit,
) {
    val density = LocalDensity.current

    BottomFade(
        gradientBrush = Brush.verticalGradient(
            colors = listOf(
                rootBackground.copy(alpha = 0f),
                rootBackground,
            ),
        ),
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

@Composable
private fun TokenDetailsBody(
    tokenDetailsUM: TokenDetailsUM,
    rootBackground: Color,
    bottomContentPadding: Dp,
    modifier: Modifier = Modifier,
    itemModifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = bottomContentPadding),
    ) {
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
        // TODO [REDACTED_TASK_KEY] Token Details Make Transaction History
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
                    actionButtons = persistentListOf(),
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
            ),
        )
    }
}
// endregion Preview