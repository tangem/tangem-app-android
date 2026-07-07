package com.tangem.features.promobanners.impl.campaigns.entity

import kotlinx.serialization.Serializable

@Serializable
sealed interface CampaignType {

    val campaignId: String

    data class ReactivationCashback(override val campaignId: String) : CampaignType
    data class WhaleSwapCashback(override val campaignId: String) : CampaignType
}