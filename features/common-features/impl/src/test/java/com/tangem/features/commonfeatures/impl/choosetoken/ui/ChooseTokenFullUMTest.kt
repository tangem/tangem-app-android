package com.tangem.features.commonfeatures.impl.choosetoken.ui

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.commonfeatures.api.choosetoken.model.BalanceFilter
import com.tangem.features.commonfeatures.api.choosetoken.model.BalanceFilterUM
import com.tangem.features.commonfeatures.api.choosetoken.model.ChooseTokenPortfolioFullBlockUM
import com.tangem.features.commonfeatures.api.choosetoken.model.EmptyReason
import com.tangem.features.commonfeatures.api.choosetoken.model.TokenListUMData
import com.tangem.features.commonfeatures.api.choosetoken.model.WalletListUM
import com.tangem.features.commonfeatures.api.choosetoken.model.WalletTabUM
import com.tangem.features.commonfeatures.impl.choosetoken.market.state.SwapMarketState
import com.tangem.features.commonfeatures.impl.choosetoken.predefined.state.PredefinedTokenItemUM
import com.tangem.features.commonfeatures.impl.choosetoken.predefined.state.PredefinedTokensUM
import com.tangem.features.commonfeatures.impl.choosetoken.ui.state.ChooserBlockUM
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ChooseTokenFullUMTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN portfolio and chooser state WHEN isPortfolioTitleShown THEN title shown only above content`(
        model: TitleModel,
    ) {
        // Act
        val actual = model.state.isPortfolioTitleShown

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        TitleModel(
            name = "portfolio loading",
            state = createState(portfolioBlock = null, chooserBlock = predefinedBlock()),
            expected = false,
        ),
        TitleModel(
            name = "predefined chooser, no eligible tokens in portfolio",
            state = createState(portfolioBlock = createPortfolio(), chooserBlock = predefinedBlock()),
            expected = false,
        ),
        TitleModel(
            name = "predefined chooser, eligible token in portfolio",
            state = createState(portfolioBlock = createPortfolio(data = tokenList()), chooserBlock = predefinedBlock()),
            expected = true,
        ),
        TitleModel(
            name = "no chooser, empty portfolio",
            state = createState(portfolioBlock = createPortfolio(), chooserBlock = null),
            expected = false,
        ),
        TitleModel(
            name = "market chooser with content, empty portfolio",
            state = createState(portfolioBlock = createPortfolio(), chooserBlock = marketBlock(marketsContent())),
            expected = false,
        ),
        TitleModel(
            name = "market chooser loading, empty portfolio",
            state = createState(portfolioBlock = createPortfolio(), chooserBlock = marketBlock(marketsLoading())),
            expected = false,
        ),
        TitleModel(
            name = "market chooser failed, empty portfolio shows empty placeholder",
            state = createState(portfolioBlock = createPortfolio(), chooserBlock = marketBlock(marketsError())),
            expected = true,
        ),
        TitleModel(
            name = "market search found nothing anywhere shows not-found placeholder",
            state = createState(
                portfolioBlock = createPortfolio(isSearching = true),
                chooserBlock = marketBlock(SwapMarketState.SearchNothingFound),
            ),
            expected = true,
        ),
        TitleModel(
            name = "balance filter hides every token",
            state = createState(
                portfolioBlock = createPortfolio(
                    data = TokenListUMData.EmptyList(reason = EmptyReason.FilteredOut),
                    balanceFilterUM = hideZeroFilter(),
                ),
                chooserBlock = marketBlock(marketsContent()),
            ),
            expected = true,
        ),
        TitleModel(
            name = "balance filter on, search finds nothing in portfolio",
            state = createState(
                portfolioBlock = createPortfolio(isSearching = true, balanceFilterUM = hideZeroFilter()),
                chooserBlock = marketBlock(marketsContent()),
            ),
            expected = true,
        ),
        TitleModel(
            name = "selected wallet empty, wallet tabs shown",
            state = createState(
                portfolioBlock = createPortfolio(walletList = WalletListUM(items = persistentListOf(walletTab()))),
                chooserBlock = predefinedBlock(),
            ),
            expected = true,
        ),
        TitleModel(
            name = "market chooser, tokens in portfolio",
            state = createState(
                portfolioBlock = createPortfolio(data = tokenList()),
                chooserBlock = marketBlock(marketsContent()),
            ),
            expected = true,
        ),
    )

    private fun createState(
        portfolioBlock: ChooseTokenPortfolioFullBlockUM?,
        chooserBlock: ChooserBlockUM?,
    ) = ChooseTokenFullUM(
        initialUM = ChooseTokenInitialUM(
            screenTitle = stringReference("Choose token"),
            isAppBarShown = false,
            onCloseClick = {},
            searchBar = SearchBarUM(
                placeholderText = stringReference("Search"),
                query = "",
                onQueryChange = {},
                isActive = false,
                onActiveChange = {},
            ),
        ),
        portfolioBlock = portfolioBlock,
        chooserBlock = chooserBlock,
    )

    private fun createPortfolio(
        data: TokenListUMData = TokenListUMData.EmptyList(),
        isSearching: Boolean = false,
        balanceFilterUM: BalanceFilterUM? = null,
        walletList: WalletListUM = WalletListUM(items = persistentListOf()),
    ) = ChooseTokenPortfolioFullBlockUM(
        walletList = walletList,
        isBalanceHidden = false,
        isSearching = isSearching,
        tokensListData = data,
        balanceFilterUM = balanceFilterUM,
    )

    private fun hideZeroFilter() = BalanceFilterUM(selected = BalanceFilter.HideZero, onOptionSelected = {})

    private fun walletTab() = WalletTabUM(
        text = stringReference("Wallet 1"),
        count = null,
        isSelected = true,
        onClick = {},
        deviceIcon = mockk(),
    )

    private fun tokenList() = TokenListUMData.TokenList(
        tokensList = persistentListOf(TokensListItemUM.Text(id = "usdt", text = stringReference("USDT"))),
        totalTokensCount = 1,
    )

    private fun predefinedBlock() = ChooserBlockUM.Predefined(
        PredefinedTokensUM(
            items = persistentListOf(
                PredefinedTokenItemUM(
                    id = "usdt_ethereum",
                    symbol = "USDT",
                    networkName = stringReference("Ethereum"),
                    networkId = "ethereum",
                    iconUrl = null,
                    onAddClick = {},
                ),
            ),
        ),
    )

    private fun marketBlock(state: SwapMarketState) = ChooserBlockUM.Market(state)

    private fun marketsContent() = SwapMarketState.Content(
        items = persistentListOf(),
        total = 0,
        loadMore = {},
        onItemClick = {},
        visibleIdsChanged = {},
        marketsTitle = stringReference("Markets"),
        shouldAssetsCount = true,
    )

    private fun marketsLoading() = SwapMarketState.Loading(
        marketsTitle = stringReference("Markets"),
        shouldAssetsCount = true,
    )

    private fun marketsError() = SwapMarketState.LoadingError(
        onRetryClicked = {},
        marketsTitle = stringReference("Markets"),
        shouldAssetsCount = true,
    )

    internal data class TitleModel(
        val name: String,
        val state: ChooseTokenFullUM,
        val expected: Boolean,
    ) {
        override fun toString(): String = name
    }
}