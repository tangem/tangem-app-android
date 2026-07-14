package com.tangem.data.promo.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.promotion.models.PromotionsResponse.PromotionDto.All
import com.tangem.datasource.api.promotion.models.PromotionsResponse.PromotionDto.PromoToken
import com.tangem.datasource.api.promotion.models.PromotionsResponse.PromotionDto.Timeline
import com.tangem.domain.promo.models.PromoCampaignId
import com.tangem.domain.promo.models.PromoPayoutToken
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test

internal class PromoCampaignConverterTest {

    private val campaign = PromoCampaignId.WhaleSwapCashback

    @Test
    fun `GIVEN dto with tokens WHEN toAvailable THEN maps tokens and timeline`() {
        // Arrange
        val all = All(
            timeline = Timeline(start = "2026-06-23T00:00:00.000Z", end = "2026-08-31T20:59:59.000Z"),
            tokens = listOf(
                PromoToken(
                    tokenId = "tether",
                    tokenAddress = "0xdac1",
                    tokenSymbol = "USDT",
                    tokenName = "Tether USD",
                    networkId = "ethereum",
                    decimals = 6,
                ),
            ),
            status = "active",
            link = "",
        )

        // Act
        val result = PromoCampaignConverter.toAvailable(campaign, all)

        // Assert
        assertThat(result.campaign).isEqualTo(campaign)
        assertThat(result.payoutTokens).containsExactly(
            PromoPayoutToken(
                tokenId = "tether",
                tokenAddress = "0xdac1",
                tokenSymbol = "USDT",
                tokenName = "Tether USD",
                networkId = "ethereum",
                decimals = 6,
            ),
        )
        assertThat(result.timeline.start).isEqualTo(Instant.parse("2026-06-23T00:00:00.000Z"))
        assertThat(result.timeline.end).isEqualTo(Instant.parse("2026-08-31T20:59:59.000Z"))
    }

    @Test
    fun `GIVEN dto with null tokens WHEN toAvailable THEN empty payout list`() {
        // Arrange
        val all = All(
            timeline = Timeline(start = "2026-06-23T00:00:00.000Z", end = "2026-08-31T20:59:59.000Z"),
            tokens = null,
            status = "active",
            link = null,
        )

        // Act
        val result = PromoCampaignConverter.toAvailable(campaign, all)

        // Assert
        assertThat(result.payoutTokens).isEmpty()
    }
}