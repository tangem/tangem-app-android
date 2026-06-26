package com.tangem.data.marketing.store

import androidx.datastore.preferences.core.emptyPreferences
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.tangem.datasource.api.marketing.models.BannerDto
import com.tangem.datasource.api.marketing.models.CampaignDto
import com.tangem.datasource.api.marketing.models.MarketingCampaignsCacheEntry
import com.tangem.datasource.api.marketing.models.MarketingCampaignsResponse
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MarketingStoresTest {

    private val dataStore = MockStateDataStore(default = emptyPreferences())
    private val appPreferencesStore = AppPreferencesStore(
        moshi = Moshi.Builder().add(LocalBigDecimalAdapter()).build(),
        dispatchers = TestingCoroutineDispatcherProvider(),
        preferencesDataStore = dataStore,
    )
    private val cacheStore = DefaultMarketingCampaignsCacheStore(appPreferencesStore)
    private val dismissStore = DefaultMarketingDismissStore(appPreferencesStore)

    @BeforeEach
    fun reset() {
        runBlocking { dataStore.updateData { emptyPreferences() } }
    }

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

    /** Local adapter for [BigDecimal] required by Moshi to handle [CampaignDto.minAmount]/[CampaignDto.maxAmount]. */
    private class LocalBigDecimalAdapter {
        @FromJson
        fun fromJson(value: String): BigDecimal = BigDecimal(value)

        @ToJson
        fun toJson(value: BigDecimal): String = value.toPlainString()
    }
}