package com.tangem.data.marketing.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.marketing.models.BannerDto
import com.tangem.datasource.api.marketing.models.CampaignDto
import com.tangem.datasource.api.marketing.models.CampaignTokenDto
import com.tangem.domain.marketing.models.MarketingBanner
import com.tangem.domain.marketing.models.MarketingCampaignTarget
import com.tangem.domain.marketing.models.MarketingScreenType
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class MarketingCampaignConverterTest {

    private val converter = MarketingCampaignConverter()

    private fun banner(uiType: String = "standalone", isDismissible: Boolean = true) = BannerDto(
        uiType = uiType,
        text = "Cashback",
        icon = "https://x/star.webp",
        iconAlign = "left",
        bgColor = "#FF0011",
        deeplink = "https://tangem.com",
        isDismissible = isDismissible,
    )

    @Test
    fun `GIVEN swap campaign WHEN convert THEN mapped with amounts and no targets`() {
        // Arrange
        val dto = CampaignDto(
            id = 12, type = "swap", priority = 1,
            minAmount = BigDecimal(50), maxAmount = BigDecimal(300),
            providerIds = listOf("provider1"), tokens = null,
            banner = banner(uiType = "linked_to_provider"),
        )

        // Act
        val result = converter.convert(dto)

        // Assert
        assertThat(result.id).isEqualTo(12)
        assertThat(result.type).isEqualTo(MarketingScreenType.SWAP)
        assertThat(result.minAmount).isEqualTo(BigDecimal(50))
        assertThat(result.banner.uiType).isEqualTo(MarketingBanner.UiType.LINKED_TO_PROVIDER)
        assertThat(result.banner.iconAlign).isEqualTo(MarketingBanner.IconAlign.LEFT)
        assertThat(result.targets).isEmpty()
    }

    @Test
    fun `GIVEN token_details campaign WHEN convert THEN network targets mapped`() {
        // Arrange
        val dto = CampaignDto(
            id = 1, type = "token_details", priority = 2, banner = banner(),
            tokens = listOf(CampaignTokenDto(networkId = "ethereum", contractAddress = "0xA0b8")),
        )

        // Act
        val targets = converter.convert(dto).targets

        // Assert
        assertThat(targets).containsExactly(
            MarketingCampaignTarget.NetworkContract(networkId = "ethereum", contractAddress = "0xA0b8"),
        )
    }

    @Test
    fun `GIVEN markets campaign WHEN convert THEN coingecko targets mapped`() {
        // Arrange
        val dto = CampaignDto(
            id = 1, type = "token_markets", priority = 1, banner = banner(),
            tokens = listOf(CampaignTokenDto(id = "1696501400")),
        )

        // Act
        val targets = converter.convert(dto).targets

        // Assert
        assertThat(targets).containsExactly(MarketingCampaignTarget.CoingeckoId(id = "1696501400"))
    }

    @Test
    fun `GIVEN linked_to_provider without providerIds WHEN convertListIgnoreErrors THEN dropped`() {
        // Arrange
        val invalid = CampaignDto(
            id = 1, type = "onramp", priority = 1, providerIds = emptyList(),
            banner = banner(uiType = "linked_to_provider"),
        )
        val valid = CampaignDto(id = 2, type = "onramp", priority = 2, banner = banner())

        // Act
        val result = converter.convertListIgnoreErrors(listOf(invalid, valid))

        // Assert
        assertThat(result.map { it.id }).containsExactly(2)
    }

    @Test
    fun `GIVEN unknown type WHEN convertListIgnoreErrors THEN dropped`() {
        // Arrange
        val dto = CampaignDto(id = 1, type = "carousel", priority = 1, banner = banner())

        // Act
        val result = converter.convertListIgnoreErrors(listOf(dto))

        // Assert
        assertThat(result).isEmpty()
    }
}