package com.tangem.domain.promo.models

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class PromoCampaignIdTest {

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