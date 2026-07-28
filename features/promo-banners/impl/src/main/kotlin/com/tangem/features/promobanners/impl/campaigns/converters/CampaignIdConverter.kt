package com.tangem.features.promobanners.impl.campaigns.converters

import com.tangem.domain.promo.models.PromoCampaignId
import com.tangem.features.promobanners.impl.campaigns.entity.CampaignType
import com.tangem.utils.converter.Converter
import javax.inject.Inject

internal class CampaignIdConverter @Inject constructor() :
    Converter<String, CampaignType?> {

    override fun convert(value: String): CampaignType? {
        return when (value) {
            PromoCampaignId.ReactivationCashback.slug -> CampaignType.ReactivationCashback(campaignId = value)
            PromoCampaignId.WhaleSwapCashback.slug -> CampaignType.WhaleSwapCashback(campaignId = value)
            else -> null
        }
    }
}