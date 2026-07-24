package com.tangem.features.foryou.impl.model.transformer

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.ds.tabs.TangemSegmentedPickerUM
import com.tangem.core.ui.ds2.filter.TangemFilterItemUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.foryou.impl.components.state.MarketChartUM
import com.tangem.features.foryou.impl.entity.EarnOpportunitiesUM
import com.tangem.features.foryou.impl.entity.ForYouTokenListItemUM
import com.tangem.features.foryou.impl.entity.ForYouUM
import com.tangem.features.foryou.impl.entity.PortfolioReviewUM
import com.tangem.features.foryou.impl.entity.asSingleForYouGroup
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.junit.jupiter.api.Test

internal class ApplyExpandedAssetsTransformerTest {

    @Test
    fun `GIVEN asset id in expanded set WHEN transform THEN only that item is expanded`() {
        // Arrange
        val state = state(portfolioItems = listOf(listItem(id = "btc"), listItem(id = "eth")))
        val transformer = ApplyExpandedAssetsTransformer(
            expandedAssetIds = setOf("btc"),
            section = ApplyExpandedAssetsTransformer.Section.PortfolioReview,
        )

        // Act
        val result = transformer.transform(state)

        // Assert
        val items = (result.portfolioReviewUM as PortfolioReviewUM.Content).tokenList
        assertThat(items.map { it.tokenRowUM.id to it.isExpanded })
            .containsExactly("btc" to true, "eth" to false)
            .inOrder()
    }

    @Test
    fun `GIVEN asset id removed from expanded set WHEN transform THEN item collapses`() {
        // Arrange
        val state = state(portfolioItems = listOf(listItem(id = "btc", isExpanded = true)))
        val transformer = ApplyExpandedAssetsTransformer(
            expandedAssetIds = emptySet(),
            section = ApplyExpandedAssetsTransformer.Section.PortfolioReview,
        )

        // Act
        val result = transformer.transform(state)

        // Assert
        val items = (result.portfolioReviewUM as PortfolioReviewUM.Content).tokenList
        assertThat(items.single().isExpanded).isFalse()
    }

    @Test
    fun `GIVEN expansion already matches WHEN transform THEN previous state instance is returned`() {
        // Untouched state identity lets downstream distinct-until-changed / recomposition skip entirely.
        // Arrange
        val state = state(portfolioItems = listOf(listItem(id = "btc", isExpanded = true), listItem(id = "eth")))
        val transformer = ApplyExpandedAssetsTransformer(
            expandedAssetIds = setOf("btc"),
            section = ApplyExpandedAssetsTransformer.Section.PortfolioReview,
        )

        // Act
        val result = transformer.transform(state)

        // Assert
        assertThat(result).isSameInstanceAs(state)
    }

    @Test
    fun `GIVEN one item toggles WHEN transform THEN untouched items keep their instances`() {
        // Sibling rows must keep identity so their keyed composition groups skip recomposition.
        // Arrange
        val untouched = listItem(id = "eth")
        val state = state(portfolioItems = listOf(listItem(id = "btc"), untouched))
        val transformer = ApplyExpandedAssetsTransformer(
            expandedAssetIds = setOf("btc"),
            section = ApplyExpandedAssetsTransformer.Section.PortfolioReview,
        )

        // Act
        val result = transformer.transform(state)

        // Assert
        val items = (result.portfolioReviewUM as PortfolioReviewUM.Content).tokenList
        assertThat(items[1]).isSameInstanceAs(untouched)
    }

    @Test
    fun `GIVEN non-expandable item id in expanded set WHEN transform THEN item stays collapsed`() {
        // Arrange
        val state = state(portfolioItems = listOf(listItem(id = "other", isExpandable = false)))
        val transformer = ApplyExpandedAssetsTransformer(
            expandedAssetIds = setOf("other"),
            section = ApplyExpandedAssetsTransformer.Section.PortfolioReview,
        )

        // Act
        val result = transformer.transform(state)

        // Assert
        val items = (result.portfolioReviewUM as PortfolioReviewUM.Content).tokenList
        assertThat(items.single().isExpanded).isFalse()
    }

    @Test
    fun `GIVEN portfolio review section WHEN transform THEN earn section is untouched`() {
        // Arrange — the same id exists in both sections; only the addressed section may change
        val state = state(
            portfolioItems = listOf(listItem(id = "btc")),
            earnItems = listOf(listItem(id = "btc")),
        )
        val transformer = ApplyExpandedAssetsTransformer(
            expandedAssetIds = setOf("btc"),
            section = ApplyExpandedAssetsTransformer.Section.PortfolioReview,
        )

        // Act
        val result = transformer.transform(state)

        // Assert
        assertThat(result.earnOpportunities).isSameInstanceAs(state.earnOpportunities)
        val earnItems = (result.earnOpportunities as EarnOpportunitiesUM.Content).tokenList.flatMap { it.items }
        assertThat(earnItems.single().isExpanded).isFalse()
    }

    @Test
    fun `GIVEN earn section WHEN transform THEN expansion applies to earn list only`() {
        // Arrange
        val state = state(
            portfolioItems = listOf(listItem(id = "account-1")),
            earnItems = listOf(listItem(id = "account-1")),
        )
        val transformer = ApplyExpandedAssetsTransformer(
            expandedAssetIds = setOf("account-1"),
            section = ApplyExpandedAssetsTransformer.Section.EarnOpportunities,
        )

        // Act
        val result = transformer.transform(state)

        // Assert
        assertThat(result.portfolioReviewUM).isSameInstanceAs(state.portfolioReviewUM)
        val earnItems = (result.earnOpportunities as EarnOpportunitiesUM.Content).tokenList.flatMap { it.items }
        assertThat(earnItems.single().isExpanded).isTrue()
    }

    @Test
    fun `GIVEN Loading portfolio section WHEN transform THEN it rebuilds as Loading with the item expanded`() {
        // Expansion can arrive while the first data emission is still pending, so the rebuild must
        // preserve the Loading subtype rather than collapse it into Content.
        // Arrange
        val state = loadingState(portfolioItems = listOf(listItem(id = "btc")))
        val transformer = ApplyExpandedAssetsTransformer(
            expandedAssetIds = setOf("btc"),
            section = ApplyExpandedAssetsTransformer.Section.PortfolioReview,
        )

        // Act
        val result = transformer.transform(state)

        // Assert
        val portfolio = result.portfolioReviewUM
        assertThat(portfolio).isInstanceOf(PortfolioReviewUM.Loading::class.java)
        assertThat(portfolio.tokenList.single().isExpanded).isTrue()
    }

    @Test
    fun `GIVEN Loading earn section WHEN transform THEN it rebuilds as Loading with the item expanded`() {
        // Arrange
        val state = loadingState(earnItems = listOf(listItem(id = "account-1")))
        val transformer = ApplyExpandedAssetsTransformer(
            expandedAssetIds = setOf("account-1"),
            section = ApplyExpandedAssetsTransformer.Section.EarnOpportunities,
        )

        // Act
        val result = transformer.transform(state)

        // Assert
        val earn = result.earnOpportunities
        assertThat(earn).isInstanceOf(EarnOpportunitiesUM.Loading::class.java)
        assertThat(earn.tokenList.flatMap { it.items }.single().isExpanded).isTrue()
    }

    private fun listItem(
        id: String,
        isExpanded: Boolean = false,
        isExpandable: Boolean = true,
    ): ForYouTokenListItemUM = ForYouTokenListItemUM(
        tokenRowUM = TangemTokenRowUM.Loading(id = id),
        tokenList = persistentListOf(),
        isExpanded = isExpanded,
        isExpandable = isExpandable,
        segmentColor = null,
    )

    private fun state(
        portfolioItems: List<ForYouTokenListItemUM> = emptyList(),
        earnItems: List<ForYouTokenListItemUM> = emptyList(),
    ): ForYouUM = ForYouUM(
        portfolioReviewUM = PortfolioReviewUM.Content(
            tokenList = persistentListOf(*portfolioItems.toTypedArray()),
            marketChartUM = MarketChartUM.NoData(
                title = stringReference("No data"),
                donutText = stringReference("No data"),
            ),
            onAddFundsClick = null,
        ),
        earnOpportunities = EarnOpportunitiesUM.Content(
            tokenList = earnItems.toPersistentList().asSingleForYouGroup(),
            subtitleRes = 0,
            potentialReward = null,
            potentialRewardType = null,
            onAllEarnTokensClick = {},
        ),
        notifications = persistentListOf(),
        periodPickerUM = TangemSegmentedPickerUM(persistentListOf()),
        onPeriodClick = {},
        portfolioFilter = TangemFilterItemUM.Loading(id = "portfolio_selector"),
    )

    private fun loadingState(
        portfolioItems: List<ForYouTokenListItemUM> = emptyList(),
        earnItems: List<ForYouTokenListItemUM> = emptyList(),
    ): ForYouUM = ForYouUM(
        portfolioReviewUM = PortfolioReviewUM.Loading(
            tokenList = persistentListOf(*portfolioItems.toTypedArray()),
            marketChartUM = MarketChartUM.NoData(
                title = stringReference("No data"),
                donutText = stringReference("No data"),
            ),
        ),
        earnOpportunities = EarnOpportunitiesUM.Loading(
            tokenList = earnItems.toPersistentList().asSingleForYouGroup(),
        ),
        notifications = persistentListOf(),
        periodPickerUM = TangemSegmentedPickerUM(persistentListOf()),
        onPeriodClick = {},
        portfolioFilter = TangemFilterItemUM.Loading(id = "portfolio_selector"),
    )
}