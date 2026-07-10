package com.tangem.features.promobanners.impl.campaigns.entity

import kotlinx.serialization.Serializable

@Serializable
internal sealed class CampaignType {

    abstract val campaignId: String

    @Serializable
    data class ReactivationCashback(override val campaignId: String) : CampaignType()

    @Serializable
    data class WhaleSwapCashback(override val campaignId: String) : CampaignType()
}