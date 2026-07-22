package com.tangem.features.foryou.impl.model.transformer

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.common.ui.account.toUM
import com.tangem.core.ui.ds.tabs.TangemSegmentUM
import com.tangem.core.ui.ds.tabs.TangemSegmentedPickerUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.Account
import com.tangem.features.foryou.impl.components.state.MarketChartUM
import com.tangem.features.foryou.impl.entity.EarnOpportunitiesUM
import com.tangem.features.foryou.impl.entity.ForYouTokenListItemUM
import com.tangem.features.foryou.impl.entity.ForYouUM
import com.tangem.features.foryou.impl.entity.PortfolioReviewUM
import com.tangem.features.foryou.impl.model.ForYouNotification
import com.tangem.features.foryou.impl.model.ForYouSelectedPortfolio
import com.tangem.features.foryou.impl.model.converter.ForYouPeriod
import com.tangem.features.foryou.impl.model.converter.earnOpportunities.createEarnCurrency
import com.tangem.features.foryou.impl.model.converter.earnOpportunities.createPortfolioStatus
import com.tangem.features.foryou.impl.model.converter.earnOpportunities.createSelectedPortfolio
import com.tangem.features.foryou.impl.model.converter.earnOpportunities.createStatus
import com.tangem.test.mock.MockAccounts
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class SetPortfolioReviewTransformerTest {

    @Nested
    inner class Sections {

        @Test
        fun `GIVEN pre-built section UMs WHEN transform THEN both are set on the state`() {
            // Arrange
            val portfolioReview = contentPortfolioReview()
            val earnOpportunities = contentEarnOpportunities()
            val transformer = createTransformer(
                portfolioReviewUM = portfolioReview,
                earnOpportunitiesUM = earnOpportunities,
            )

            // Act
            val result = transformer.transform(loadingState())

            // Assert
            assertThat(result.portfolioReviewUM).isEqualTo(portfolioReview)
            assertThat(result.earnOpportunities).isEqualTo(earnOpportunities)
        }
    }

    @Nested
    inner class Expansion {

        @Test
        fun `GIVEN expanded asset ids WHEN transform THEN expansion is re-applied to the converted sections`() {
            // Arrange — converters always build items collapsed
            val portfolioReview = contentPortfolioReview().copy(
                tokenList = persistentListOf(listItem(id = "btc"), listItem(id = "eth")),
            )
            val earnOpportunities = contentEarnOpportunities().copy(
                tokenList = persistentListOf(listItem(id = "account-1")),
            )
            val transformer = createTransformer(
                portfolioReviewUM = portfolioReview,
                earnOpportunitiesUM = earnOpportunities,
                expandedPortfolioReviewAssetIds = setOf("btc"),
                expandedEarnOpportunitiesAssetIds = setOf("account-1"),
            )

            // Act
            val result = transformer.transform(loadingState())

            // Assert — a data refresh must not collapse what the user expanded
            val portfolioItems = (result.portfolioReviewUM as PortfolioReviewUM.Content).tokenList
            assertThat(portfolioItems.map { it.tokenRowUM.id to it.isExpanded })
                .containsExactly("btc" to true, "eth" to false)
                .inOrder()
            val earnItems = (result.earnOpportunities as EarnOpportunitiesUM.Content).tokenList
            assertThat(earnItems.single().isExpanded).isTrue()
        }

        @Test
        fun `GIVEN non-expandable item id in expanded set WHEN transform THEN item stays collapsed`() {
            // Arrange
            val portfolioReview = contentPortfolioReview().copy(
                tokenList = persistentListOf(listItem(id = "other", isExpandable = false)),
            )
            val transformer = createTransformer(
                portfolioReviewUM = portfolioReview,
                expandedPortfolioReviewAssetIds = setOf("other"),
            )

            // Act
            val result = transformer.transform(loadingState())

            // Assert
            val items = (result.portfolioReviewUM as PortfolioReviewUM.Content).tokenList
            assertThat(items.single().isExpanded).isFalse()
        }

        @Test
        fun `GIVEN expanded set changes after construction WHEN transform THEN the latest value is applied`() {
            // The providers must be read at transform time, not captured at construction — otherwise an
            // expand click landing after the transformer is built but before it runs would be lost.
            // Arrange
            val portfolioReview = contentPortfolioReview().copy(
                tokenList = persistentListOf(listItem(id = "btc")),
            )
            var expandedIds = emptySet<String>()
            val transformer = SetPortfolioReviewTransformer(
                selectedPortfolio = createSelectedPortfolio(),
                portfolioReviewUM = portfolioReview,
                earnOpportunitiesUM = contentEarnOpportunities(),
                expandedPortfolioReviewAssetIds = { expandedIds },
                expandedEarnOpportunitiesAssetIds = { emptySet() },
            )
            // Change what the provider returns after the transformer already exists
            expandedIds = setOf("btc")

            // Act
            val result = transformer.transform(loadingState())

            // Assert — the post-construction value took effect
            val item = (result.portfolioReviewUM as PortfolioReviewUM.Content).tokenList.single()
            assertThat(item.isExpanded).isTrue()
        }
    }

    @Nested
    inner class PeriodPicker {

        @Test
        fun `GIVEN previous state is Loading WHEN transform THEN period picker is created with Day selected`() {
            // Arrange
            val transformer = createTransformer()

            // Act
            val result = transformer.transform(loadingState())

            // Assert
            assertThat(result.periodPickerUM.items).hasSize(3)
            assertThat(result.periodPickerUM.initialSelectedItem).isEqualTo(result.periodPickerUM.items.first())
        }

        @Test
        fun `GIVEN previous state is Loading WHEN transform THEN picker items map to ForYouPeriod entries`() {
            // Pins the picker to the ForYouPeriod enum: a reorder or id/title change in the enum must
            // surface here rather than silently drift (the old hardcoded Day/Week/Month is gone).
            // Arrange
            val transformer = createTransformer()

            // Act
            val result = transformer.transform(loadingState())

            // Assert — ids and titles come from the enum, in declaration order
            assertThat(result.periodPickerUM.items.map { it.id })
                .containsExactlyElementsIn(ForYouPeriod.entries.map { it.id })
                .inOrder()
            assertThat(result.periodPickerUM.items.map { it.title })
                .containsExactlyElementsIn(ForYouPeriod.entries.map { it.title })
                .inOrder()
            assertThat(result.periodPickerUM.initialSelectedItem?.id).isEqualTo(ForYouPeriod.Day.id)
        }

        @Test
        fun `GIVEN previous state is Content WHEN transform THEN user's picker selection is carried over`() {
            // Arrange — the user has already switched to the "Week" segment
            val week = TangemSegmentUM(id = "1", title = stringReference("Week"))
            val pickerWithSelection = TangemSegmentedPickerUM(
                items = persistentListOf(TangemSegmentUM(id = "0", title = stringReference("Day")), week),
                initialSelectedItem = week,
            )
            val prevState = loadingState().copy(
                portfolioReviewUM = contentPortfolioReview(),
                periodPickerUM = pickerWithSelection,
            )
            val transformer = createTransformer()

            // Act
            val result = transformer.transform(prevState)

            // Assert — a balance refresh must not reset the selection back to Day
            assertThat(result.periodPickerUM).isEqualTo(pickerWithSelection)
        }
    }

    @Nested
    inner class Notifications {

        @Test
        fun `GIVEN total balance from outdated source WHEN transform THEN outdated-data notification is emitted`() {
            // Arrange
            val transformer = createTransformer(
                selectedPortfolio = createSelectedPortfolio(
                    totalFiatBalance = loaded(BigDecimal("10"), source = StatusSource.ONLY_CACHE),
                ),
            )

            // Act
            val result = transformer.transform(loadingState())

            // Assert
            assertThat(result.notifications).containsExactly(ForYouNotification.UsedOutdatedData)
        }

        @Test
        fun `GIVEN total balance from actual source WHEN transform THEN no notification is emitted`() {
            // Arrange
            val transformer = createTransformer(
                selectedPortfolio = createSelectedPortfolio(
                    totalFiatBalance = loaded(BigDecimal("10"), source = StatusSource.ACTUAL),
                ),
            )

            // Act
            val result = transformer.transform(loadingState())

            // Assert
            assertThat(result.notifications).isEmpty()
        }

        @Test
        fun `GIVEN total balance not yet loaded WHEN transform THEN no notification is emitted`() {
            // Arrange
            val transformer = createTransformer(
                selectedPortfolio = createSelectedPortfolio(totalFiatBalance = TotalFiatBalance.Failed),
            )

            // Act
            val result = transformer.transform(loadingState())

            // Assert
            assertThat(result.notifications).isEmpty()
        }
    }

    @Nested
    inner class PortfolioSelectorLabel {

        @Test
        fun `GIVEN every account selected WHEN transform THEN label is All accounts`() {
            // Arrange
            val account1 = MockAccounts.createAccount(derivationIndex = 1)
            val account2 = MockAccounts.createAccount(derivationIndex = 2)
            val transformer = createTransformer(
                selectedPortfolio = createSelectedPortfolio(
                    accountStatusWithCurrency(account1),
                    accountStatusWithCurrency(account2),
                    totalAccountsCount = 2,
                ),
            )

            // Act
            val result = transformer.transform(loadingState())

            // Assert
            assertThat(result.portfolioSelectorLabel).isEqualTo(stringReference("All accounts"))
        }

        @Test
        fun `GIVEN no account selected WHEN transform THEN label is All accounts`() {
            // Arrange
            val transformer = createTransformer(
                selectedPortfolio = createSelectedPortfolio(totalAccountsCount = 1),
            )

            // Act
            val result = transformer.transform(loadingState())

            // Assert
            assertThat(result.portfolioSelectorLabel).isEqualTo(stringReference("All accounts"))
        }

        @Test
        fun `GIVEN a single account selected out of many WHEN transform THEN label is the account name`() {
            // Arrange
            val account1 = MockAccounts.createAccount(derivationIndex = 1)
            val transformer = createTransformer(
                selectedPortfolio = createSelectedPortfolio(
                    accountStatusWithCurrency(account1),
                    totalAccountsCount = 2,
                ),
            )

            // Act
            val result = transformer.transform(loadingState())

            // Assert
            assertThat(result.portfolioSelectorLabel).isEqualTo(account1.accountName.toUM().value)
        }

        @Test
        fun `GIVEN a subset of several accounts selected WHEN transform THEN label is the selected count`() {
            // Arrange
            val account1 = MockAccounts.createAccount(derivationIndex = 1)
            val account2 = MockAccounts.createAccount(derivationIndex = 2)
            val transformer = createTransformer(
                selectedPortfolio = createSelectedPortfolio(
                    accountStatusWithCurrency(account1),
                    accountStatusWithCurrency(account2),
                    totalAccountsCount = 3,
                ),
            )

            // Act
            val result = transformer.transform(loadingState())

            // Assert
            assertThat(result.portfolioSelectorLabel).isEqualTo(stringReference("2 accounts"))
        }
    }

    /** A selected account carrying a single currency, so it shows up in the portfolio's account statuses. */
    private fun accountStatusWithCurrency(account: Account.CryptoPortfolio) = createPortfolioStatus(
        currencies = listOf(createStatus(createEarnCurrency())),
        account = account,
    )

    private fun createTransformer(
        selectedPortfolio: ForYouSelectedPortfolio = createSelectedPortfolio(),
        portfolioReviewUM: PortfolioReviewUM = contentPortfolioReview(),
        earnOpportunitiesUM: EarnOpportunitiesUM = contentEarnOpportunities(),
        expandedPortfolioReviewAssetIds: Set<String> = emptySet(),
        expandedEarnOpportunitiesAssetIds: Set<String> = emptySet(),
    ) = SetPortfolioReviewTransformer(
        selectedPortfolio = selectedPortfolio,
        portfolioReviewUM = portfolioReviewUM,
        earnOpportunitiesUM = earnOpportunitiesUM,
        expandedPortfolioReviewAssetIds = { expandedPortfolioReviewAssetIds },
        expandedEarnOpportunitiesAssetIds = { expandedEarnOpportunitiesAssetIds },
    )

    private fun contentPortfolioReview(): PortfolioReviewUM.Content = PortfolioReviewUM.Content(
        tokenList = persistentListOf(),
        marketChartUM = noDataChart(),
        onAddFundsClick = null,
    )

    private fun contentEarnOpportunities(): EarnOpportunitiesUM.Content = EarnOpportunitiesUM.Content(
        tokenList = persistentListOf(),
        subtitleRes = 0,
        potentialReward = null,
        potentialRewardType = null,
        onAllEarnTokensClick = {},
    )

    private fun noDataChart(): MarketChartUM.NoData = MarketChartUM.NoData(
        title = stringReference("No data"),
        donutText = stringReference("No data"),
    )

    private fun listItem(
        id: String,
        isExpandable: Boolean = true,
    ): ForYouTokenListItemUM = ForYouTokenListItemUM(
        tokenRowUM = TangemTokenRowUM.Content(
            id = id,
            headIconUM = mockk(relaxed = true),
            titleUM = TangemTokenRowUM.TitleUM.Content(text = stringReference(id)),
            subtitleUM = TangemTokenRowUM.SubtitleUM.Content(text = stringReference(id)),
            topEndContentUM = TangemTokenRowUM.EndContentUM.Empty,
            bottomEndContentUM = TangemTokenRowUM.EndContentUM.Empty,
            onItemClick = null,
            onItemLongClick = null,
        ),
        tokenList = persistentListOf(),
        isExpanded = false,
        isExpandable = isExpandable,
    )

    private fun loaded(amount: BigDecimal, source: StatusSource = StatusSource.ACTUAL): TotalFiatBalance.Loaded =
        TotalFiatBalance.Loaded(amount = amount, source = source)

    private fun loadingState(): ForYouUM = ForYouUM(
        portfolioReviewUM = PortfolioReviewUM.Loading(
            tokenList = persistentListOf(),
            marketChartUM = noDataChart(),
        ),
        earnOpportunities = EarnOpportunitiesUM.Loading(tokenList = persistentListOf()),
        notifications = persistentListOf(),
        periodPickerUM = TangemSegmentedPickerUM(persistentListOf()),
        onPeriodClick = {},
        portfolioSelectorLabel = stringReference("All accounts"),
        onSelectPortfolioClick = {},
    )
}