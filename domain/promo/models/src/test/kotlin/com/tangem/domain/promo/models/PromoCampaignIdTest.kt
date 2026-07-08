package com.tangem.domain.promo.models

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class PromoCampaignIdTest {

    @Test
    fun `GIVEN known deeplink id WHEN fromDeeplinkId THEN returns campaign`() {
        assertThat(PromoCampaignId.fromDeeplinkId(1)).isEqualTo(PromoCampaignId.WhaleSwapCashback)
        assertThat(PromoCampaignId.fromDeeplinkId(2)).isEqualTo(PromoCampaignId.ReactivationCashback)
    }

    @Test
    fun `GIVEN unknown deeplink id WHEN fromDeeplinkId THEN returns null`() {
        assertThat(PromoCampaignId.fromDeeplinkId(99)).isNull()
    }

    @Test
    fun `GIVEN known slug WHEN fromSlug THEN returns campaign`() {
        assertThat(PromoCampaignId.fromSlug("whale-swap-cashback")).isEqualTo(PromoCampaignId.WhaleSwapCashback)
        assertThat(PromoCampaignId.fromSlug("reactivation-cashback")).isEqualTo(PromoCampaignId.ReactivationCashback)
    }

    @Test
    fun `GIVEN unknown slug WHEN fromSlug THEN returns null`() {
        assertThat(PromoCampaignId.fromSlug("nope")).isNull()
    }
}