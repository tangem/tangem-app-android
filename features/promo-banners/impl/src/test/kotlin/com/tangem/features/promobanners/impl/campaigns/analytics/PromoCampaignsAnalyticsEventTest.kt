package com.tangem.features.promobanners.impl.campaigns.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.features.promobanners.impl.campaigns.entity.CampaignType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PromoCampaignsAnalyticsEventTest {

    @Test
    fun `GIVEN any event WHEN created THEN category is Promotion`() {
        // Arrange
        val events = listOf(
            PromoCampaignsAnalyticsEvent.PromotionScreenOpened(campaignType = whaleSwap),
            PromoCampaignsAnalyticsEvent.EnrollButtonClicked(
                campaignType = whaleSwap,
                token = "USDT",
                blockchain = "Ethereum",
            ),
            PromoCampaignsAnalyticsEvent.AlreadyEnrolledScreenOpened(),
        )

        // Assert
        assertThat(events.map { it.category }).containsExactly("Promotion", "Promotion", "Promotion")
    }

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

    @Test
    fun `GIVEN AlreadyEnrolledScreenOpened WHEN created THEN event name is correct and no params`() {
        // Act
        val event = PromoCampaignsAnalyticsEvent.AlreadyEnrolledScreenOpened()

        // Assert
        assertThat(event.event).isEqualTo("Already Enrolled Screen Opened")
        assertThat(event.params).isEmpty()
    }

    private fun provideCampaignTypes() = listOf(
        CampaignNameModel(campaignType = whaleSwap, expectedAnalyticsName = "Cashback"),
        CampaignNameModel(campaignType = reactivation, expectedAnalyticsName = "Reactivation"),
    )

    internal data class CampaignNameModel(
        val campaignType: CampaignType,
        val expectedAnalyticsName: String,
    ) {
        override fun toString(): String = "${campaignType::class.simpleName} -> $expectedAnalyticsName"
    }

    private companion object {
        val whaleSwap = CampaignType.WhaleSwapCashback(campaignId = "whale")
        val reactivation = CampaignType.ReactivationCashback(campaignId = "reactivation")
    }
}