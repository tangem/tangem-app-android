package com.tangem.datasource.api.marketing

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.datasource.api.common.adapter.BigDecimalAdapter
import com.tangem.datasource.api.marketing.models.MarketingCampaignsResponse
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class MarketingCampaignsResponseTest {

    private val moshi = Moshi.Builder().add(BigDecimalAdapter()).build()
    private val adapter = moshi.adapter(MarketingCampaignsResponse::class.java)

    @Test
    fun `GIVEN swap response json WHEN parsed THEN fields mapped`() {
        // Arrange
        val json = """
            {"campaigns":[{"id":12,"type":"swap","priority":1,"minAmount":50,"maxAmount":300,
            "providerIds":["provider1"],
            "banner":{"uiType":"linked_to_provider","text":"Cashback 4 U","icon":"https://x/star.webp",
            "bgColor":"#FF0011","deeplink":"https://tangem.com","dismissible":true}}]}
        """.trimIndent()

        // Act
        val result = adapter.fromJson(json)!!

        // Assert
        val campaign = result.campaigns.single()
        assertThat(campaign.id).isEqualTo(12)
        assertThat(campaign.type).isEqualTo("swap")
        assertThat(campaign.minAmount).isEqualTo(BigDecimal(50))
        assertThat(campaign.providerIds).containsExactly("provider1")
        assertThat(campaign.banner.uiType).isEqualTo("linked_to_provider")
        assertThat(campaign.banner.isDismissible).isTrue()
        assertThat(campaign.tokens).isNull()
    }

    @Test
    fun `GIVEN token_details response WHEN parsed THEN network targets mapped`() {
        // Arrange
        val json = """
            {"campaigns":[{"id":12,"type":"token_details","priority":1,
            "tokens":[{"networkId":"ethereum","contractAddress":"0xA0b8"}],
            "banner":{"uiType":"standalone","dismissible":false}}]}
        """.trimIndent()

        // Act
        val campaign = adapter.fromJson(json)!!.campaigns.single()

        // Assert
        val token = campaign.tokens!!.single()
        assertThat(token.networkId).isEqualTo("ethereum")
        assertThat(token.contractAddress).isEqualTo("0xA0b8")
        assertThat(token.id).isNull()
        assertThat(campaign.minAmount).isNull()
    }

    @Test
    fun `GIVEN markets response WHEN parsed THEN coingecko ids mapped`() {
        // Arrange
        val json = """
            {"campaigns":[{"id":12,"type":"token_markets","priority":1,
            "tokens":[{"id":"1696501400"},{"id":"3296501412"}],
            "banner":{"uiType":"standalone","dismissible":true}}]}
        """.trimIndent()

        // Act
        val tokens = adapter.fromJson(json)!!.campaigns.single().tokens!!

        // Assert
        assertThat(tokens.map { it.id }).containsExactly("1696501400", "3296501412")
        assertThat(tokens.all { it.networkId == null }).isTrue()
    }
}