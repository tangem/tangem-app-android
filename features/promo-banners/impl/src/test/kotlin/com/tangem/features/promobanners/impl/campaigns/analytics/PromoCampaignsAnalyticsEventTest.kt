package com.tangem.features.promobanners.impl.campaigns.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.promo.models.PromoMatchMode
import com.tangem.features.promobanners.impl.campaigns.analytics.PromoCampaignsAnalyticsEvent.AlreadyEnrolledScreenOpened
import com.tangem.features.promobanners.impl.campaigns.analytics.PromoCampaignsAnalyticsEvent.EnrollError
import com.tangem.features.promobanners.impl.campaigns.analytics.PromoCampaignsAnalyticsEvent.PayoutToken
import com.tangem.features.promobanners.impl.campaigns.entity.CampaignType
import com.tangem.features.promobanners.impl.campaigns.v2.createPromoCampaign
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PromoCampaignsAnalyticsEventTest {

    @Test
    fun `GIVEN any event WHEN created THEN category is Promotion`() {
        // Arrange
        val campaign = createPromoCampaign()
        val events = listOf(
            PromoCampaignsAnalyticsEvent.PromotionScreenOpened(campaignType = whaleSwap),
            PromoCampaignsAnalyticsEvent.PromotionScreenOpened(campaign = campaign),
            PromoCampaignsAnalyticsEvent.EnrollButtonClicked(
                campaignType = whaleSwap,
                token = "USDT",
                blockchain = "Ethereum",
            ),
            PromoCampaignsAnalyticsEvent.EnrollButtonClicked(campaign = campaign, payoutToken = null, walletsCount = 1),
            AlreadyEnrolledScreenOpened(campaign = "whale-swap", source = AlreadyEnrolledScreenOpened.Source.Entry),
            PromoCampaignsAnalyticsEvent.EnrollSuccessful(
                campaign = campaign,
                payoutToken = null,
                walletsEnrolled = 1,
                walletsTotal = 1,
            ),
            EnrollError(campaign = campaign, reason = EnrollError.Reason.Technical, errorCode = null),
        )

        // Assert
        assertThat(events.map { it.category }.distinct()).containsExactly("Promotion")
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class LegacyCampaign {

        @ParameterizedTest
        @MethodSource("provideCampaignTypes")
        fun `GIVEN campaign type WHEN PromotionScreenOpened THEN event name and Screen param are correct`(
            model: CampaignNameModel,
        ) {
            // Act
            val event = PromoCampaignsAnalyticsEvent.PromotionScreenOpened(campaignType = model.campaignType)

            // Assert
            assertThat(event.event).isEqualTo("Promotion Screen Opened")
            assertThat(event.params).containsExactly("Screen", model.expectedAnalyticsName)
        }

        @ParameterizedTest
        @MethodSource("provideCampaignTypes")
        fun `GIVEN campaign type WHEN EnrollButtonClicked THEN event name and params are correct`(
            model: CampaignNameModel,
        ) {
            // Act
            val event = PromoCampaignsAnalyticsEvent.EnrollButtonClicked(
                campaignType = model.campaignType,
                token = "USDT",
                blockchain = "Ethereum",
            )

            // Assert
            assertThat(event.event).isEqualTo("Enroll Button Clicked")
            assertThat(event.params).containsExactly(
                "Campaign", model.expectedAnalyticsName,
                "Token", "USDT",
                "Blockchain", "Ethereum",
            )
        }

        private fun provideCampaignTypes() = listOf(
            CampaignNameModel(campaignType = whaleSwap, expectedAnalyticsName = "Cashback"),
            CampaignNameModel(campaignType = reactivation, expectedAnalyticsName = "Reactivation"),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PromotionScreenOpened {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN backend campaign WHEN PromotionScreenOpened THEN campaign name and mode params sent`(
            model: ScreenOpenedModel,
        ) {
            // Arrange
            val campaign = createPromoCampaign(
                name = "whale-swap-cashback",
                isPayoutNeeded = model.isPayoutNeeded,
                matchMode = model.matchMode,
            )

            // Act
            val event = PromoCampaignsAnalyticsEvent.PromotionScreenOpened(campaign = campaign)

            // Assert
            assertThat(event.event).isEqualTo("Promotion Screen Opened")
            assertThat(event.params).containsExactly(
                "Screen", "whale-swap-cashback",
                "Campaign", "whale-swap-cashback",
                "Match Mode", model.expectedMatchMode,
                "Payout", model.expectedPayout,
            )
        }

        private fun provideTestModels() = listOf(
            ScreenOpenedModel(PromoMatchMode.Strict, isPayoutNeeded = true, "Strict", expectedPayout = "Yes"),
            ScreenOpenedModel(PromoMatchMode.LinkedWallet, isPayoutNeeded = false, "Linked Wallet", expectedPayout = "No"),
        )
    }

    @Nested
    inner class EnrollButtonClicked {

        @Test
        fun `GIVEN payout token WHEN EnrollButtonClicked THEN token and wallets count sent`() {
            // Arrange
            val campaign = createPromoCampaign(name = "whale-swap-cashback", isPayoutNeeded = true)

            // Act
            val event = PromoCampaignsAnalyticsEvent.EnrollButtonClicked(
                campaign = campaign,
                payoutToken = PayoutToken(symbol = "USDT", blockchain = "Ethereum"),
                walletsCount = 2,
            )

            // Assert
            assertThat(event.event).isEqualTo("Enroll Button Clicked")
            assertThat(event.params).containsExactly(
                "Campaign", "whale-swap-cashback",
                "Token", "USDT",
                "Blockchain", "Ethereum",
                "Match Mode", "Strict",
                "Wallets Count", "2",
            )
        }

        @Test
        fun `GIVEN no payout token WHEN EnrollButtonClicked THEN token and blockchain omitted`() {
            // Act
            val event = PromoCampaignsAnalyticsEvent.EnrollButtonClicked(
                campaign = createPromoCampaign(name = "no-payout"),
                payoutToken = null,
                walletsCount = 1,
            )

            // Assert
            assertThat(event.params).containsExactly(
                "Campaign", "no-payout",
                "Match Mode", "Strict",
                "Wallets Count", "1",
            )
        }
    }

    @Nested
    inner class AlreadyEnrolled {

        @Test
        fun `GIVEN enroll response source WHEN AlreadyEnrolledScreenOpened THEN campaign and source sent`() {
            // Act
            val event = AlreadyEnrolledScreenOpened(
                campaign = "whale-swap-cashback",
                source = AlreadyEnrolledScreenOpened.Source.EnrollResponse,
            )

            // Assert
            assertThat(event.event).isEqualTo("Already Enrolled Screen Opened")
            assertThat(event.params).containsExactly(
                "Campaign", "whale-swap-cashback",
                "Source", "Enroll Response",
            )
        }
    }

    @Nested
    inner class EnrollResult {

        @Test
        fun `GIVEN partial success with payout WHEN EnrollSuccessful THEN wallet counts and token sent`() {
            // Act
            val event = PromoCampaignsAnalyticsEvent.EnrollSuccessful(
                campaign = createPromoCampaign(name = "linked", matchMode = PromoMatchMode.LinkedWallet),
                payoutToken = PayoutToken(symbol = "USDT", blockchain = "Ethereum"),
                walletsEnrolled = 1,
                walletsTotal = 2,
            )

            // Assert
            assertThat(event.event).isEqualTo("Enroll Successful")
            assertThat(event.params).containsExactly(
                "Campaign", "linked",
                "Match Mode", "Linked Wallet",
                "Token", "USDT",
                "Blockchain", "Ethereum",
                "Wallets Enrolled", "1",
                "Wallets Total", "2",
            )
        }

        @Test
        fun `GIVEN http code WHEN EnrollError THEN reason and error code sent`() {
            // Act
            val event = EnrollError(
                campaign = createPromoCampaign(name = "whale"),
                reason = EnrollError.Reason.NotActiveYet,
                errorCode = 409,
            )

            // Assert
            assertThat(event.event).isEqualTo("Enroll Error")
            assertThat(event.params).containsExactly(
                "Campaign", "whale",
                "Reason", "Not Active Yet",
                "Error Code", "409",
            )
        }

        @Test
        fun `GIVEN no http code WHEN EnrollError THEN error code omitted`() {
            // Act
            val event = EnrollError(
                campaign = createPromoCampaign(name = "whale"),
                reason = EnrollError.Reason.Technical,
                errorCode = null,
            )

            // Assert
            assertThat(event.params).containsExactly("Campaign", "whale", "Reason", "Technical")
        }
    }

    internal data class CampaignNameModel(
        val campaignType: CampaignType,
        val expectedAnalyticsName: String,
    ) {
        override fun toString(): String = "${campaignType::class.simpleName} -> $expectedAnalyticsName"
    }

    internal data class ScreenOpenedModel(
        val matchMode: PromoMatchMode,
        val isPayoutNeeded: Boolean,
        val expectedMatchMode: String,
        val expectedPayout: String,
    )

    private companion object {
        val whaleSwap = CampaignType.WhaleSwapCashback(campaignId = "whale")
        val reactivation = CampaignType.ReactivationCashback(campaignId = "reactivation")
    }
}