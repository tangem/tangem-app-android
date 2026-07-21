package com.tangem.features.foryou.impl.model.transformer

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.account.toUM
import com.tangem.core.ui.ds.tabs.TangemSegmentUM
import com.tangem.core.ui.ds.tabs.TangemSegmentedPickerUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.Account
import com.tangem.features.foryou.impl.components.state.MarketChartUM
import com.tangem.features.foryou.impl.entity.EarnOpportunitiesUM
import com.tangem.features.foryou.impl.entity.ForYouUM
import com.tangem.features.foryou.impl.entity.PortfolioReviewUM
import com.tangem.features.foryou.impl.model.ForYouNotification
import com.tangem.features.foryou.impl.model.ForYouSelectedPortfolio
import com.tangem.features.foryou.impl.model.converter.earnOpportunities.createEarnCurrency
import com.tangem.features.foryou.impl.model.converter.earnOpportunities.createPortfolioStatus
import com.tangem.features.foryou.impl.model.converter.earnOpportunities.createSelectedPortfolio
import com.tangem.features.foryou.impl.model.converter.earnOpportunities.createStatus
import com.tangem.test.mock.MockAccounts
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
    ) = SetPortfolioReviewTransformer(
        selectedPortfolio = selectedPortfolio,
        portfolioReviewUM = portfolioReviewUM,
        earnOpportunitiesUM = earnOpportunitiesUM,
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