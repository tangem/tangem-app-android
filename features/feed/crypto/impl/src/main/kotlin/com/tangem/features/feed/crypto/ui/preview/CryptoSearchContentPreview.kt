package com.tangem.features.feed.crypto.ui.preview

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.charts.state.MarketChartLook
import com.tangem.common.ui.markets.tokenselector.AccountHeaderData
import com.tangem.common.ui.markets.tokenselector.BalanceDisplayState
import com.tangem.common.ui.markets.tokenselector.TokenSelectorSectionUM
import com.tangem.common.ui.markets.tokenselector.UserAssetItemUM
import com.tangem.core.ui.components.marketprice.PriceChangeState
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRowMarket
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.feed.crypto.ui.CryptoSearchContent
import com.tangem.features.feed.crypto.ui.state.CryptoSearchUM
import com.tangem.features.feed.crypto.ui.state.MarketPulseItemUM
import com.tangem.features.feed.crypto.ui.state.MarketSearchUM
import com.tangem.features.feed.crypto.ui.state.PortfolioSearchUM
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Suppress("MagicNumber")
private object CryptoSearchPreviewFixtures {

    /** Both sections filled — the Figma reference state (11885-669694). */
    val portfolioAndMarket: CryptoSearchUM
        get() = CryptoSearchUM(
            query = "usdt",
            portfolio = PortfolioSearchUM.Content(multiWalletSections()),
            market = MarketSearchUM.Content(items = marketItems(), loadMore = {}),
        )

    /** Portfolio arrived first; the market list is still filling in. */
    val portfolioWhileMarketLoads: CryptoSearchUM
        get() = CryptoSearchUM(
            query = "usdt",
            portfolio = PortfolioSearchUM.Content(multiWalletSections()),
            market = MarketSearchUM.Loading,
        )

    /** Nothing held — the common case for a token the user does not own. */
    val marketOnly: CryptoSearchUM
        get() = CryptoSearchUM(
            query = "usdt",
            portfolio = PortfolioSearchUM.Empty,
            market = MarketSearchUM.Content(items = marketItems(), loadMore = {}),
        )

    /** Low-cap matches held back behind the notification. */
    val marketWithUnderCapLimit: CryptoSearchUM
        get() = CryptoSearchUM(
            query = "usdt",
            portfolio = PortfolioSearchUM.Empty,
            market = MarketSearchUM.Content(
                items = marketItems(),
                loadMore = {},
                shouldShowUnderMarketCapLimitNotification = true,
            ),
        )

    /** Holdings matched but the market did not — the market header must not appear alone. */
    val portfolioWithoutMarket: CryptoSearchUM
        get() = CryptoSearchUM(
            query = "usdt",
            portfolio = PortfolioSearchUM.Content(multiWalletSections()),
            market = MarketSearchUM.NotFound,
        )

    val nothingFound: CryptoSearchUM
        get() = CryptoSearchUM(
            query = "usdt",
            portfolio = PortfolioSearchUM.Empty,
            market = MarketSearchUM.NotFound,
        )

    val marketFailed: CryptoSearchUM
        get() = CryptoSearchUM(
            query = "usdt",
            portfolio = PortfolioSearchUM.Empty,
            market = MarketSearchUM.Error(onRetry = {}),
        )

    private fun portfolioRow(name: String, network: String, crypto: String, fiat: String) = UserAssetItemUM.Single(
        id = "$name-$network",
        icon = TangemIconUM.Empty,
        tokenName = name,
        tokenSymbol = "USDT",
        fiatRate = null,
        priceChangeState = PriceChangeState.Unknown,
        balanceState = BalanceDisplayState.Loaded(
            cryptoBalance = stringReference(crypto),
            fiatBalance = stringReference(fiat),
        ),
        isBalanceHidden = false,
        networkName = network,
        onClick = {},
    )

    private fun accountGroup(accountName: String, rows: List<UserAssetItemUM.Single>) =
        TokenSelectorSectionUM.TokenGroup(
            accountHeader = AccountHeaderData(
                accountName = stringReference(accountName),
                cryptoPortfolioIcon = CryptoPortfolioIcon.ofDefaultCustomAccount(),
            ),
            items = rows.toImmutableList(),
        )

    private fun marketRow(name: String, ticker: String, price: String, isDown: Boolean) = MarketPulseItemUM(
        row = TangemTokenRowMarket.State.Content(
            id = ticker,
            icon = TangemTokenIcon.UiState.Token(TangemTokenIcon.State(url = null)),
            title = stringReference(name),
            ticker = stringReference(ticker),
            position = stringReference("3"),
            capitalization = stringReference("$187.116 B"),
            price = stringReference(price),
            priceChange = TangemPriceChange.State(
                value = stringReference("0.01%"),
                direction = if (isDown) TangemPriceChange.Direction.Down else TangemPriceChange.Direction.Up,
            ),
            onClick = {},
        ),
        chartData = null,
        chartType = if (isDown) MarketChartLook.Type.Falling else MarketChartLook.Type.Growing,
    )

    private fun marketItems() = persistentListOf(
        marketRow(name = "Tether", ticker = "USDT", price = "$0,9988", isDown = true),
        marketRow(name = "USD Coin", ticker = "USDC", price = "$0,9998", isDown = false),
        marketRow(name = "Dai", ticker = "DAI", price = "$0,9991", isDown = true),
    )

    private fun multiWalletSections() = persistentListOf(
        TokenSelectorSectionUM.WalletHeader(
            walletName = "New Wallet",
            deviceIcon = DeviceIconUM.Stub(cardsCount = 2),
        ),
        accountGroup(
            accountName = "Main account",
            rows = listOf(
                portfolioRow(name = "Tether", network = "Tron network", crypto = "2,900.1393 USDT", fiat = "$2,900.13"),
            ),
        ),
        accountGroup(
            accountName = "Savings",
            rows = listOf(
                portfolioRow(name = "Tether", network = "Tron network", crypto = "2,900.1393 USDT", fiat = "$2,900.13"),
            ),
        ),
        TokenSelectorSectionUM.WalletHeader(
            walletName = "My Wallet",
            deviceIcon = DeviceIconUM.Stub(cardsCount = 1),
        ),
        accountGroup(
            accountName = "Family wallet",
            rows = listOf(
                portfolioRow(name = "Tether", network = "Ethereum network", crypto = "863.09 USDT", fiat = "$863.09"),
            ),
        ),
    )
}

@Composable
private fun CryptoSearchPreviewHost(state: CryptoSearchUM) {
    CryptoSearchContent(
        state = state,
        listState = rememberLazyListState(),
        contentPadding = PaddingValues(top = 8.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary),
    )
}

@Composable
@Preview(name = "01 Portfolio + market", showBackground = true, widthDp = 402, heightDp = 800)
@Preview(
    name = "01 Portfolio + market (night)",
    showBackground = true,
    widthDp = 402,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
private fun CryptoSearchContentPreview_PortfolioAndMarket() {
    TangemThemePreviewRedesign {
        CryptoSearchPreviewHost(state = CryptoSearchPreviewFixtures.portfolioAndMarket)
    }
}

@Composable
@Preview(name = "02 Portfolio + market loading", showBackground = true, widthDp = 402, heightDp = 800)
@Preview(
    name = "02 Portfolio + market loading (night)",
    showBackground = true,
    widthDp = 402,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
private fun CryptoSearchContentPreview_PortfolioWhileMarketLoads() {
    TangemThemePreviewRedesign {
        CryptoSearchPreviewHost(state = CryptoSearchPreviewFixtures.portfolioWhileMarketLoads)
    }
}

@Composable
@Preview(name = "03 Market only", showBackground = true, widthDp = 402, heightDp = 800)
@Preview(
    name = "03 Market only (night)",
    showBackground = true,
    widthDp = 402,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
private fun CryptoSearchContentPreview_MarketOnly() {
    TangemThemePreviewRedesign {
        CryptoSearchPreviewHost(state = CryptoSearchPreviewFixtures.marketOnly)
    }
}

@Composable
@Preview(name = "04 Under market cap notification", showBackground = true, widthDp = 402, heightDp = 800)
@Preview(
    name = "04 Under market cap notification (night)",
    showBackground = true,
    widthDp = 402,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
private fun CryptoSearchContentPreview_UnderCapLimit() {
    TangemThemePreviewRedesign {
        CryptoSearchPreviewHost(state = CryptoSearchPreviewFixtures.marketWithUnderCapLimit)
    }
}

@Composable
@Preview(name = "05 Portfolio without market", showBackground = true, widthDp = 402, heightDp = 800)
@Preview(
    name = "05 Portfolio without market (night)",
    showBackground = true,
    widthDp = 402,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
private fun CryptoSearchContentPreview_PortfolioWithoutMarket() {
    TangemThemePreviewRedesign {
        CryptoSearchPreviewHost(state = CryptoSearchPreviewFixtures.portfolioWithoutMarket)
    }
}

@Composable
@Preview(name = "06 Nothing found", showBackground = true, widthDp = 402, heightDp = 800)
@Preview(
    name = "06 Nothing found (night)",
    showBackground = true,
    widthDp = 402,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
private fun CryptoSearchContentPreview_NothingFound() {
    TangemThemePreviewRedesign {
        CryptoSearchPreviewHost(state = CryptoSearchPreviewFixtures.nothingFound)
    }
}

@Composable
@Preview(name = "07 Market failed", showBackground = true, widthDp = 402, heightDp = 800)
@Preview(
    name = "07 Market failed (night)",
    showBackground = true,
    widthDp = 402,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
private fun CryptoSearchContentPreview_MarketFailed() {
    TangemThemePreviewRedesign {
        CryptoSearchPreviewHost(state = CryptoSearchPreviewFixtures.marketFailed)
    }
}