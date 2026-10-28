package com.tangem.data.marketing.converter

import com.tangem.datasource.api.marketing.models.BannerDto
import com.tangem.datasource.api.marketing.models.CampaignDto
import com.tangem.datasource.api.marketing.models.CampaignTokenDto
import com.tangem.domain.marketing.models.MarketingBanner
import com.tangem.domain.marketing.models.MarketingCampaign
import com.tangem.domain.marketing.models.MarketingCampaignTarget
import com.tangem.domain.marketing.models.MarketingScreenType
import com.tangem.utils.converter.Converter

internal class MarketingCampaignConverter : Converter<CampaignDto, MarketingCampaign> {

    override fun convert(value: CampaignDto): MarketingCampaign {
        val type = requireNotNull(MarketingScreenType.fromValue(value.type)) { "Unknown campaign type: ${value.type}" }
        val banner = convertBanner(value.banner)

        require(banner.uiType != MarketingBanner.UiType.LINKED_TO_PROVIDER || !value.providerIds.isNullOrEmpty()) {
            "linked_to_provider campaign ${value.id} has no providerIds"
        }
        require(!banner.hasCallToAction() || banner.hasTextAndButtonText()) {
            "${value.banner.uiType} campaign ${value.id} has no text or buttonText"
        }

        return MarketingCampaign(
            id = value.id,
            type = type,
            priority = value.priority,
            startAt = value.startAt,
            endAt = value.endAt,
            minAmount = value.minAmount,
            maxAmount = value.maxAmount,
            providerIds = value.providerIds,
            banner = banner,
            targets = value.tokens.orEmpty().mapNotNull(::convertTarget),
        )
    }

    private fun convertBanner(dto: BannerDto) = MarketingBanner(
        uiType = when (dto.uiType) {
            "standalone" -> MarketingBanner.UiType.STANDALONE
            "linked_to_provider" -> MarketingBanner.UiType.LINKED_TO_PROVIDER
            "pseudo_provider" -> MarketingBanner.UiType.PSEUDO_PROVIDER
            "standalone_cta" -> MarketingBanner.UiType.STANDALONE_CTA
            else -> throw IllegalArgumentException("Unknown banner uiType: ${dto.uiType}")
        },
        text = dto.text,
        iconUrl = dto.icon,
        iconAlign = when (dto.iconAlign) {
            "left" -> MarketingBanner.IconAlign.LEFT
            "right" -> MarketingBanner.IconAlign.RIGHT
            else -> null
        },
        bgColor = dto.bgColor,
        deeplink = dto.deeplink,
        isDismissible = dto.isDismissible,
        categoryLabel = dto.categoryLabel,
        subtitle = dto.subtitle,
        etaLabel = dto.etaLabel,
        partnerLabel = dto.partnerLabel,
        buttonText = dto.buttonText,
        hasGlow = dto.hasGlow,
    )

    private fun MarketingBanner.hasCallToAction(): Boolean =
        uiType == MarketingBanner.UiType.PSEUDO_PROVIDER || uiType == MarketingBanner.UiType.STANDALONE_CTA

    private fun MarketingBanner.hasTextAndButtonText(): Boolean = !text.isNullOrBlank() && !buttonText.isNullOrBlank()

    private fun convertTarget(dto: CampaignTokenDto): MarketingCampaignTarget? {
        val coingeckoId = dto.id
        val networkId = dto.networkId
        return when {
            coingeckoId != null -> MarketingCampaignTarget.CoingeckoId(id = coingeckoId)
            // contractAddress may be null — that's a native coin of [networkId], not an invalid target.
            networkId != null -> MarketingCampaignTarget.NetworkContract(
                networkId = networkId,
                contractAddress = dto.contractAddress,
            )
            else -> null
        }
    }
}