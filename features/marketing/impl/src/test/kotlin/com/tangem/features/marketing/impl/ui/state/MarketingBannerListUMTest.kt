package com.tangem.features.marketing.impl.ui.state

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.marketing.models.MarketingBanner.RecommendedSlot
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test

internal class MarketingBannerListUMTest {

    @Test
    fun `GIVEN hidden WHEN replacedSlots THEN empty`() {
        assertThat(MarketingBannerListUM.Hidden.replacedSlots()).isEmpty()
    }

    @Test
    fun `GIVEN banners with and without slot WHEN replacedSlots THEN only declared slots`() {
        // Arrange
        val state = MarketingBannerListUM.Content(
            persistentListOf(banner(1, RecommendedSlot.BEST), banner(2, null), banner(3, RecommendedSlot.FASTEST)),
        )

        // Act
        val actual = state.replacedSlots()

        // Assert
        assertThat(actual).containsExactly(RecommendedSlot.BEST, RecommendedSlot.FASTEST)
    }

    @Test
    fun `GIVEN assigned and unassigned banners WHEN selecting surfaces THEN each banner appears only in its surface`() {
        // Arrange
        val best = banner(1, RecommendedSlot.BEST)
        val unassigned = banner(2, null)
        val fastest = banner(3, RecommendedSlot.FASTEST)
        val state = MarketingBannerListUM.Content(persistentListOf(best, unassigned, fastest))

        // Act
        val aboveOffers = state.forSlot(null)
        val bestSlot = state.forSlot(RecommendedSlot.BEST)
        val fastestSlot = state.forSlot(RecommendedSlot.FASTEST)

        // Assert
        assertThat(aboveOffers).isEqualTo(MarketingBannerListUM.Content(persistentListOf(unassigned)))
        assertThat(bestSlot).isEqualTo(MarketingBannerListUM.Content(persistentListOf(best)))
        assertThat(fastestSlot).isEqualTo(MarketingBannerListUM.Content(persistentListOf(fastest)))
    }

    @Test
    fun `GIVEN two banners for one slot WHEN selecting slot THEN only the higher priority banner shows`() {
        // Arrange
        val first = banner(1, RecommendedSlot.BEST)
        val second = banner(2, RecommendedSlot.BEST)
        val state = MarketingBannerListUM.Content(persistentListOf(first, second))

        // Act
        val actual = state.forSlot(RecommendedSlot.BEST)

        // Assert
        assertThat(actual).isEqualTo(MarketingBannerListUM.Content(persistentListOf(first)))
    }

    private fun banner(id: Int, slot: RecommendedSlot?) = MarketingBannerUM(
        campaignId = id,
        text = "t",
        iconUrl = null,
        iconAlign = MarketingBannerUM.IconAlign.LEFT,
        isDismissible = false,
        deeplink = null,
        replacesSlot = slot,
        style = MarketingBannerUM.Style.PSEUDO_PROVIDER,
    )
}