package com.tangem.features.promobanners.impl.campaigns.converters

import com.tangem.features.promobanners.impl.campaigns.entity.CampaignType
import com.tangem.utils.converter.Converter
import javax.inject.Inject

internal class CampaignIdConverter @Inject constructor() :
    Converter<String, CampaignType?> {

    override fun convert(value: String): CampaignType? {
        return when (value) {
            CAMPAIGN_ID_REACTIVATION -> CampaignType.ReactivationCashback(campaignId = value)
            CAMPAIGN_ID_WHALE -> CampaignType.WhaleSwapCashback(campaignId = value)
            else -> null
        }
    }

    private companion object {
        const val CAMPAIGN_ID_WHALE = "1"
        const val CAMPAIGN_ID_REACTIVATION = "2"
    }
}