package com.tangem.features.promobanners.impl.campaigns.entity

import kotlinx.serialization.Serializable

@Serializable
internal sealed class CampaignsBottomSheetConfig {

    @Serializable
    data object NotActive : CampaignsBottomSheetConfig()

    @Serializable
    data class Enrolled(
        val campaignType: CampaignType,
    ) : CampaignsBottomSheetConfig()

    @Serializable
    data class Activate(
        val campaignType: CampaignType,
    ) : CampaignsBottomSheetConfig()
}