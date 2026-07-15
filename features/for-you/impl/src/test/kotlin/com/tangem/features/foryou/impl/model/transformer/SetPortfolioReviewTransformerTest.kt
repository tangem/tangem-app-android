package com.tangem.features.foryou.impl.model.transformer

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds.tabs.TangemSegmentUM
import com.tangem.core.ui.ds.tabs.TangemSegmentedPickerUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.features.foryou.impl.components.state.MarketChartUM
import com.tangem.features.foryou.impl.entity.EarnOpportunitiesUM
import com.tangem.features.foryou.impl.entity.ForYouUM
import com.tangem.features.foryou.impl.entity.PortfolioReviewUM
import com.tangem.features.foryou.impl.model.ForYouNotification
import io.mockk.every
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
                accountStatusList = accountStatusList(loaded(BigDecimal("10"), source = StatusSource.ONLY_CACHE)),
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
                accountStatusList = accountStatusList(loaded(BigDecimal("10"), source = StatusSource.ACTUAL)),
            )

            // Act
            val result = transformer.transform(loadingState())

            // Assert
            assertThat(result.notifications).isEmpty()
        }

        @Test
        fun `GIVEN null account status list WHEN transform THEN no notification is emitted`() {
            // Arrange
            val transformer = createTransformer(accountStatusList = null)

            // Act
            val result = transformer.transform(loadingState())

            // Assert
            assertThat(result.notifications).isEmpty()
        }
    }

    private fun createTransformer(
        accountStatusList: AccountStatusList? = null,
        portfolioReviewUM: PortfolioReviewUM = contentPortfolioReview(),
        earnOpportunitiesUM: EarnOpportunitiesUM = contentEarnOpportunities(),
    ) = SetPortfolioReviewTransformer(
        accountStatusList = accountStatusList,
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

    private fun accountStatusList(totalFiatBalance: TotalFiatBalance): AccountStatusList = mockk {
        every { this@mockk.totalFiatBalance } returns totalFiatBalance
    }

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
    )
}