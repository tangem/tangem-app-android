package com.tangem.data.marketing.store

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.adapter
import com.tangem.datasource.api.marketing.models.BannerDto
import com.tangem.datasource.api.marketing.models.CampaignDto
import com.tangem.datasource.api.marketing.models.CampaignTokenDto
import com.tangem.datasource.api.marketing.models.MarketingCampaignsCacheEntry
import com.tangem.datasource.api.marketing.models.MarketingCampaignsResponse
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.datastore.legacyDataStoreMoshi
import com.tangem.test.core.datastore.readStoredJson
import com.tangem.test.core.datastore.writeStoredJson
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

/**
 * Guards the stored format of the marketing campaigns cache: files written by the Moshi serializer must read back
 * unchanged after the store moved to kotlinx, and new writes keep that format, so the Moshi serializer that is still
 * used while the kotlinx DataStore toggle is off reads them too.
 */
@OptIn(ExperimentalStdlibApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MarketingCampaignsCacheStoredFormatTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN cache WHEN encoded by Moshi THEN matches the stored format`(model: FormatModel) {
        // Act
        val actual = legacyDataStoreMoshi.adapter<Map<String, MarketingCampaignsCacheEntry>>().toJson(model.value)

        // Assert
        assertThat(actual).isEqualTo(model.json)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN stored format WHEN decoded by Moshi THEN cache is kept`(model: FormatModel) {
        // Act
        val actual = legacyDataStoreMoshi.adapter<Map<String, MarketingCampaignsCacheEntry>>().fromJson(model.json)

        // Assert
        assertThat(actual).isEqualTo(model.value)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN stored format WHEN read from DataStore THEN cache is kept`(model: FormatModel) {
        // Act
        val actual = readStoredJson(
            json = model.json,
            defaultValue = emptyMap<String, MarketingCampaignsCacheEntry>(),
        )

        // Assert
        assertThat(actual).isEqualTo(model.value)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN cache WHEN written to DataStore THEN keeps the stored format`(model: FormatModel) {
        // Act
        val actual = writeStoredJson(value = model.value, defaultValue = emptyMap())

        // Assert
        assertThat(actual).isEqualTo(model.json)
    }

    private fun provideTestModels() = listOf(
        FormatModel(
            value = mapOf(
                "promo" to MarketingCampaignsCacheEntry(
                    eTag = "\"etag\"",
                    response = MarketingCampaignsResponse(
                        campaigns = listOf(
                            CampaignDto(
                                id = 7,
                                type = "promo",
                                priority = 1,
                                startAt = "2026-10-01T00:00:00Z",
                                endAt = "2026-10-31T00:00:00Z",
                                minAmount = BigDecimal("0.00000001"),
                                maxAmount = BigDecimal("100"),
                                providerIds = listOf("provider"),
                                tokens = listOf(
                                    CampaignTokenDto(networkId = "ethereum", contractAddress = "0xtoken", id = "usdt"),
                                ),
                                banner = BannerDto(
                                    uiType = "large",
                                    text = "Text",
                                    icon = "https://icon",
                                    iconAlign = "left",
                                    bgColor = "#000000",
                                    deeplink = "tangem://promo",
                                    isDismissible = true,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
            json = """{"promo":{"eTag":"\"etag\"","response":{"campaigns":[{"id":7,"type":"promo","priority":1,""" +
                """"startAt":"2026-10-01T00:00:00Z","endAt":"2026-10-31T00:00:00Z","minAmount":"1E-8",""" +
                """"maxAmount":"100","providerIds":["provider"],"tokens":[{"networkId":"ethereum",""" +
                """"contractAddress":"0xtoken","id":"usdt"}],"banner":{"uiType":"large","text":"Text",""" +
                """"icon":"https://icon","iconAlign":"left","bgColor":"#000000","deeplink":"tangem://promo",""" +
                """"dismissible":true}}]}}}""",
        ),
        FormatModel(
            value = mapOf(
                "promo" to MarketingCampaignsCacheEntry(
                    eTag = null,
                    response = MarketingCampaignsResponse(
                        campaigns = listOf(
                            CampaignDto(id = 7, type = "promo", priority = 1, banner = BannerDto(uiType = "small")),
                        ),
                    ),
                ),
            ),
            json = """{"promo":{"response":{"campaigns":[{"id":7,"type":"promo","priority":1,""" +
                """"banner":{"uiType":"small","dismissible":false}}]}}}""",
        ),
    )

    data class FormatModel(val value: Map<String, MarketingCampaignsCacheEntry>, val json: String)
}