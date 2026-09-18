package com.tangem.domain.pay.model

sealed interface TangemPayCampaignResolution {

    data class Landing(val campaign: TangemPayCampaign) : TangemPayCampaignResolution

    data object Unavailable : TangemPayCampaignResolution
}