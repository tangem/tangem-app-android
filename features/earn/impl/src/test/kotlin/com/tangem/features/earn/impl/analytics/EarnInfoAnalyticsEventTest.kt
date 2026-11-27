package com.tangem.features.earn.impl.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.earn.api.EarnEntrySource
import com.tangem.features.earn.impl.fake.fakeClaimAction
import com.tangem.features.earn.product.api.product.EarnPeriod
import com.tangem.features.earn.product.api.session.Deposit
import com.tangem.features.earn.product.api.session.EarnAction
import com.tangem.features.earn.product.api.session.Withdraw
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class EarnInfoAnalyticsEventTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class EventContract {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN info event WHEN built THEN id and params match the spec`(model: EventModel) {
            // Act
            val event = model.event

            // Assert
            assertThat(event.id).isEqualTo(model.expectedId)
            assertThat(event.params).isEqualTo(model.expectedParams)
        }

        private fun provideTestModels() = listOf(
            EventModel(
                event = EarnInfoAnalyticsEvent.InfoScreenOpened(
                    category = "ETH Lending",
                    source = EarnEntrySource.YieldSheet,
                    positionBucket = "1K-5K",
                    earnedBucket = "<50",
                    currentApy = "5.50",
                ),
                expectedId = "[ETH Lending] Info Screen Opened",
                expectedParams = mapOf(
                    "Source" to "Yield Sheet",
                    "Position Bucket" to "1K-5K",
                    "Earned Bucket" to "<50",
                    "Current APY" to "5.50",
                ),
            ),
            EventModel(
                event = EarnInfoAnalyticsEvent.InfoScreenOpened(
                    category = "ETH Lending",
                    source = EarnEntrySource.YieldSheet,
                    positionBucket = "1K-5K",
                    earnedBucket = null,
                    currentApy = "5.50",
                ),
                expectedId = "[ETH Lending] Info Screen Opened",
                expectedParams = mapOf(
                    "Source" to "Yield Sheet",
                    "Position Bucket" to "1K-5K",
                    "Current APY" to "5.50",
                ),
            ),
            EventModel(
                event = EarnInfoAnalyticsEvent.InfoScreenOpened(
                    category = "ETH Staking",
                    source = EarnEntrySource.Deeplink,
                    positionBucket = null,
                    earnedBucket = null,
                    currentApy = null,
                ),
                expectedId = "[ETH Staking] Info Screen Opened",
                expectedParams = mapOf("Source" to "Deeplink"),
            ),
            EventModel(
                event = EarnInfoAnalyticsEvent.ChartPeriod(category = "ETH Lending", period = "3M"),
                expectedId = "[ETH Lending] Button - Chart Period",
                expectedParams = mapOf("Period" to "3M"),
            ),
            EventModel(
                event = EarnInfoAnalyticsEvent.InfoScreenAction(category = "ETH Lending", action = InfoAction.AddFunds),
                expectedId = "[ETH Lending] Button - Info Screen Action",
                expectedParams = mapOf("Action" to "Add Funds"),
            ),
            EventModel(
                event = EarnInfoAnalyticsEvent.InfoScreenAction(category = "ETH Lending", action = InfoAction.Withdraw),
                expectedId = "[ETH Lending] Button - Info Screen Action",
                expectedParams = mapOf("Action" to "Withdraw"),
            ),
            EventModel(
                event = EarnInfoAnalyticsEvent.HowItWorksOpened(
                    category = "ETH Lending",
                    source = EarnEntrySource.EarnBanner,
                ),
                expectedId = "[ETH Lending] How It Works Sheet Opened",
                expectedParams = mapOf("Source" to "Earn Banner"),
            ),
            EventModel(
                event = EarnInfoAnalyticsEvent.LearnMore(
                    category = "ETH Lending",
                    source = EarnEntrySource.EarnBanner,
                ),
                expectedId = "[ETH Lending] Button - Learn More",
                expectedParams = mapOf("Source" to "Earn Banner"),
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PeriodValue {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN chart period WHEN toAnalyticsValue THEN spec string or null`(model: PeriodModel) {
            // Act
            val actual = model.input.toAnalyticsValue()

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            PeriodModel(period(id = "1m"), expected = "1M"),
            PeriodModel(period(id = "3m"), expected = "3M"),
            PeriodModel(period(id = "6m"), expected = "6M"),
            PeriodModel(period(id = "1M"), expected = "1M"),
            PeriodModel(period(id = "3M"), expected = "3M"),
            PeriodModel(period(id = "6M"), expected = "6M"),
            PeriodModel(period(id = "1y"), expected = null),
        )

        private fun period(id: String) = EarnPeriod(id = id, label = stringReference(id))
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ActionValue {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN earn action WHEN toInfoAction THEN info action or null`(model: ActionModel) {
            // Act
            val actual = model.input.toInfoAction()

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            ActionModel(Withdraw(), expected = InfoAction.Withdraw),
            ActionModel(Withdraw(isGaslessAllowed = true), expected = InfoAction.Withdraw),
            ActionModel(Deposit(), expected = InfoAction.AddFunds),
            ActionModel(Deposit(isGaslessAllowed = true), expected = InfoAction.AddFunds),
            ActionModel(fakeClaimAction(), expected = null),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InfoActionValue {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN info action WHEN value read THEN spec string`(model: InfoActionModel) {
            // Act
            val actual = model.input.value

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            InfoActionModel(InfoAction.Withdraw, expected = "Withdraw"),
            InfoActionModel(InfoAction.AddFunds, expected = "Add Funds"),
        )
    }

    internal data class EventModel(
        val event: AnalyticsEvent,
        val expectedId: String,
        val expectedParams: Map<String, String>,
    )

    internal data class PeriodModel(val input: EarnPeriod, val expected: String?) {
        override fun toString(): String = "${input.id} -> $expected"
    }

    internal data class ActionModel(val input: EarnAction, val expected: InfoAction?) {
        override fun toString(): String = "${input.id} -> $expected"
    }

    internal data class InfoActionModel(val input: InfoAction, val expected: String) {
        override fun toString(): String = "$input -> $expected"
    }
}