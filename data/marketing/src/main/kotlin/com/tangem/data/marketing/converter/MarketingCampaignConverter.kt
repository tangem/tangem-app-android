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

        return MarketingCampaign(
            id = value.id,
            type = type,
            priority = value.priority,
            minAmount = value.minAmount,
            maxAmount = value.maxAmount,
            providerIds = value.providerIds,
            banner = banner,
            targets = value.tokens.orEmpty().mapNotNull(::convertTarget),
        )
    }

    private fun convertBanner(dto: BannerDto) = MarketingBanner(
        uiType = when (dto.uiType) {
            "linked_to_provider" -> MarketingBanner.UiType.LINKED_TO_PROVIDER
            else -> MarketingBanner.UiType.STANDALONE
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
    )

    private fun convertTarget(dto: CampaignTokenDto): MarketingCampaignTarget? {
        val id = dto.id
        val networkId = dto.networkId
        val contractAddress = dto.contractAddress
        return when {
            id != null -> MarketingCampaignTarget.CoingeckoId(id = id)
            networkId != null && contractAddress != null ->
                MarketingCampaignTarget.NetworkContract(networkId = networkId, contractAddress = contractAddress)
            else -> null
        }
    }
}