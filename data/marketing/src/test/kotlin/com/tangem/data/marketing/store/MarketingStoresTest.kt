package com.tangem.data.marketing.store

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.marketing.models.BannerDto
import com.tangem.datasource.api.marketing.models.CampaignDto
import com.tangem.datasource.api.marketing.models.MarketingCampaignsCacheEntry
import com.tangem.datasource.api.marketing.models.MarketingCampaignsResponse
import com.tangem.test.core.datastore.MockStateDataStore
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class MarketingStoresTest {

    private val cacheStore = DefaultMarketingCampaignsCacheStore(
        dataStore = MockStateDataStore<Map<String, MarketingCampaignsCacheEntry>>(default = emptyMap()),
    )
    private val dismissStore = DefaultMarketingDismissStore(
        dataStore = MockStateDataStore<Set<Int>>(default = emptySet()),
    )

    private fun entry(eTag: String?) = MarketingCampaignsCacheEntry(
        eTag = eTag,
        response = MarketingCampaignsResponse(
            campaigns = listOf(
                CampaignDto(id = 1, type = "token_details", priority = 1, banner = BannerDto(uiType = "standalone")),
            ),
        ),
    )

    @Test
    fun `GIVEN no cache WHEN get THEN null`() = runTest {
        assertThat(cacheStore.get("token_details")).isNull()
    }

    @Test
    fun `GIVEN stored entry WHEN get same type THEN returns it`() = runTest {
        // Arrange
        cacheStore.store("token_details", entry(eTag = "abc"))

        // Act
        val result = cacheStore.get("token_details")

        // Assert
        assertThat(result?.eTag).isEqualTo("abc")
        assertThat(result?.response?.campaigns).hasSize(1)
        assertThat(cacheStore.get("staking")).isNull()
    }

    @Test
    fun `GIVEN no dismissed WHEN getDismissedIds THEN empty`() = runTest {
        assertThat(dismissStore.getDismissedIds()).isEmpty()
    }

    @Test
    fun `GIVEN dismissed ids WHEN dismiss again THEN accumulates without duplicates`() = runTest {
        // Act
        dismissStore.dismiss(12)
        dismissStore.dismiss(12)
        dismissStore.dismiss(34)

        // Assert
        assertThat(dismissStore.getDismissedIds()).containsExactly(12, 34)
    }
}