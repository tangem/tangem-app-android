package com.tangem.data.marketing.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.marketing.models.BannerDto
import com.tangem.datasource.api.marketing.models.CampaignDto
import com.tangem.datasource.api.marketing.models.CampaignTokenDto
import com.tangem.domain.marketing.models.MarketingBanner
import com.tangem.domain.marketing.models.MarketingCampaignTarget
import com.tangem.domain.marketing.models.MarketingScreenType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
    fun `GIVEN native coin token (null contract) WHEN convert THEN network target with null contract`() {
        // Arrange
        val dto = CampaignDto(
            id = 1, type = "yield", priority = 1, banner = banner(),
            tokens = listOf(CampaignTokenDto(networkId = "bitcoin", contractAddress = null)),
        )

        // Act
        val targets = converter.convert(dto).targets

        // Assert
        assertThat(targets).containsExactly(
            MarketingCampaignTarget.NetworkContract(networkId = "bitcoin", contractAddress = null),
        )
    }

    @Test
    fun `GIVEN markets campaign WHEN convert THEN coingecko targets mapped`() {
        // Arrange
        val dto = CampaignDto(
            id = 1, type = "markets_token", priority = 1, banner = banner(),
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

    @ParameterizedTest
    @MethodSource("provideUiTypeModels")
    fun `GIVEN known uiType WHEN convert THEN mapped to its UiType`(model: UiTypeModel) {
        // Arrange
        val dto = onrampCampaign(banner = ctaBanner(uiType = model.uiType))

        // Act
        val uiType = converter.convert(dto).banner.uiType

        // Assert
        assertThat(uiType).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN pseudo_provider with all fields WHEN convert THEN banner fully mapped`() {
        // Arrange
        val dto = onrampCampaign(banner = ctaBanner(uiType = "pseudo_provider"))

        // Act
        val banner = converter.convert(dto).banner

        // Assert
        assertThat(banner).isEqualTo(
            MarketingBanner(
                uiType = MarketingBanner.UiType.PSEUDO_PROVIDER,
                text = "Personal support manager",
                iconUrl = null,
                iconAlign = null,
                bgColor = null,
                deeplink = "tangem://support",
                isDismissible = false,
                categoryLabel = "Concierge for large purchases",
                subtitle = "Pay by bank transfer",
                etaLabel = "up to 4 hours",
                partnerLabel = "via Partner",
                buttonText = "Request",
                hasGlow = true,
            ),
        )
    }

    @Test
    fun `GIVEN unknown uiType WHEN convertListIgnoreErrors THEN only that campaign dropped`() {
        // Arrange
        val unknown = onrampCampaign(id = 1, banner = banner(uiType = "carousel"))
        val valid = onrampCampaign(id = 2, banner = banner())

        // Act
        val result = converter.convertListIgnoreErrors(listOf(unknown, valid))

        // Assert
        assertThat(result.map { it.id }).containsExactly(2)
    }

    @ParameterizedTest
    @MethodSource("provideIncompleteCtaBanners")
    fun `GIVEN call-to-action banner without text or buttonText WHEN convertListIgnoreErrors THEN dropped`(
        banner: BannerDto,
    ) {
        // Arrange
        val dto = onrampCampaign(banner = banner)

        // Act
        val result = converter.convertListIgnoreErrors(listOf(dto))

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `GIVEN standalone without buttonText WHEN convert THEN kept`() {
        // Arrange
        val dto = onrampCampaign(banner = banner(uiType = "standalone").copy(buttonText = null))

        // Act
        val result = converter.convertListIgnoreErrors(listOf(dto))

        // Assert
        assertThat(result.map { it.banner.uiType }).containsExactly(MarketingBanner.UiType.STANDALONE)
    }

    private fun onrampCampaign(id: Int = 1, banner: BannerDto) =
        CampaignDto(id = id, type = "onramp", priority = 1, banner = banner)

    private fun ctaBanner(uiType: String) = BannerDto(
        uiType = uiType,
        text = "Personal support manager",
        deeplink = "tangem://support",
        isDismissible = false,
        categoryLabel = "Concierge for large purchases",
        subtitle = "Pay by bank transfer",
        etaLabel = "up to 4 hours",
        partnerLabel = "via Partner",
        buttonText = "Request",
        hasGlow = true,
    )

    private fun provideUiTypeModels() = listOf(
        UiTypeModel(uiType = "standalone", expected = MarketingBanner.UiType.STANDALONE),
        UiTypeModel(uiType = "pseudo_provider", expected = MarketingBanner.UiType.PSEUDO_PROVIDER),
        UiTypeModel(uiType = "standalone_cta", expected = MarketingBanner.UiType.STANDALONE_CTA),
    )

    private fun provideIncompleteCtaBanners() = listOf(
        ctaBanner(uiType = "pseudo_provider").copy(text = null),
        ctaBanner(uiType = "pseudo_provider").copy(buttonText = " "),
        ctaBanner(uiType = "standalone_cta").copy(text = ""),
        ctaBanner(uiType = "standalone_cta").copy(buttonText = null),
    )

    data class UiTypeModel(val uiType: String, val expected: MarketingBanner.UiType)

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