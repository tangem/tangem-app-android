package com.tangem.domain.marketing.models

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class MarketingCampaignAmountTest {

    private fun campaign(
        type: MarketingScreenType,
        minAmount: BigDecimal? = null,
        maxAmount: BigDecimal? = null,
    ) = MarketingCampaign(
        id = 1, type = type, priority = 1, startAt = null, endAt = null,
        minAmount = minAmount, maxAmount = maxAmount, providerIds = null,
        banner = MarketingBanner(
            uiType = MarketingBanner.UiType.STANDALONE, text = "t", iconUrl = null,
            iconAlign = null, bgColor = null, deeplink = null, isDismissible = false,
        ),
        targets = emptyList(),
    )

    @Test
    fun `GIVEN no min max bounds WHEN matchesUsdAmount THEN always true`() {
        val c = campaign(MarketingScreenType.TOKEN_DETAILS, minAmount = null, maxAmount = null)
        assertThat(c.matchesUsdAmount(BigDecimal(10))).isTrue()
        assertThat(c.matchesUsdAmount(null)).isTrue()
    }

    @Test
    fun `GIVEN bounded campaign of any type WHEN amount out of range THEN false`() {
        // Bounds apply regardless of screen type (iOS parity): type no longer exempts a bounded campaign.
        val c = campaign(MarketingScreenType.TOKEN_DETAILS, minAmount = BigDecimal(50), maxAmount = BigDecimal(300))
        assertThat(c.matchesUsdAmount(BigDecimal(10))).isFalse()
        assertThat(c.matchesUsdAmount(BigDecimal(100))).isTrue()
    }

    @Test
    fun `GIVEN bounded campaign with null amount WHEN matchesUsdAmount THEN false`() {
        val c = campaign(MarketingScreenType.SWAP, minAmount = BigDecimal(50))
        assertThat(c.matchesUsdAmount(null)).isFalse()
    }

    @Test
    fun `GIVEN swap amount below min WHEN matchesUsdAmount THEN false`() {
        val c = campaign(MarketingScreenType.SWAP, minAmount = BigDecimal(50), maxAmount = BigDecimal(300))
        assertThat(c.matchesUsdAmount(BigDecimal(49))).isFalse()
    }

    @Test
    fun `GIVEN swap amount above max WHEN matchesUsdAmount THEN false`() {
        val c = campaign(MarketingScreenType.ONRAMP, minAmount = BigDecimal(50), maxAmount = BigDecimal(300))
        assertThat(c.matchesUsdAmount(BigDecimal(301))).isFalse()
    }

    @Test
    fun `GIVEN amount on boundaries WHEN matchesUsdAmount THEN true`() {
        val c = campaign(MarketingScreenType.SWAP, minAmount = BigDecimal(50), maxAmount = BigDecimal(300))
        assertThat(c.matchesUsdAmount(BigDecimal(50))).isTrue()
        assertThat(c.matchesUsdAmount(BigDecimal(300))).isTrue()
    }

    @Test
    fun `GIVEN nullable bounds WHEN matchesUsdAmount THEN only present bound applies`() {
        val onlyMin = campaign(MarketingScreenType.SWAP, minAmount = BigDecimal(50), maxAmount = null)
        assertThat(onlyMin.matchesUsdAmount(BigDecimal(10_000))).isTrue()
        assertThat(onlyMin.matchesUsdAmount(BigDecimal(10))).isFalse()
        val onlyMax = campaign(MarketingScreenType.SWAP, minAmount = null, maxAmount = BigDecimal(300))
        assertThat(onlyMax.matchesUsdAmount(BigDecimal(1))).isTrue()
        assertThat(onlyMax.matchesUsdAmount(BigDecimal(301))).isFalse()
    }
}