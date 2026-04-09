package com.tangem.feature.tokendetails.presentation.tokendetails.ui

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.account.AccountIconUM
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
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeTint
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun TokenDetailsScreen(tokenDetailsUM: TokenDetailsUM, modifier: Modifier = Modifier) {
    val topAppBarUM = tokenDetailsUM.topAppBarUM

    val statusBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getTop(this).toDp() }
    val topBarHeight = 64.dp
    val partialCollapsedHeight = topBarHeight + statusBarHeight
    val expandedHeight = TokenDetailsBalanceBlockHeight + partialCollapsedHeight

    val behavior = rememberTangemExitUntilCollapsedScrollBehavior(
        expandedHeight = expandedHeight,
        partialCollapsedHeight = partialCollapsedHeight,
    )

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
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(top = topBarHeight),
                    )
                },
                body = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(behavior.nestedScrollConnection),
                    ) {
                        // TODO [REDACTED_TASK_KEY] Token Details Make Transaction History
                    }
                },
            )
        }

        val rootBackground by LocalRootBackgroundColor.current
        val hazeIntensity by animateFloatAsState(
            targetValue = (behavior.state.collapsedFraction * 2f).coerceIn(0f, 1f),
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
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TokenDetailsScreen_Preview() {
    TangemThemePreviewRedesign {
        TokenDetailsScreen(
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
                balanceBlockUM = TokenDetailsBalanceBlockUM.Loading(
                    actionButtons = persistentListOf(),
                    tokenBalanceTypeUM = TokenBalanceTypeUM.Single,
                    currencyIconState = CurrencyIconState.Loading,
                ),
                marketPriceBlockState = MarketPriceBlockState.Loading(currencySymbol = "USDT"),
                stakingBlocksState = null,
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