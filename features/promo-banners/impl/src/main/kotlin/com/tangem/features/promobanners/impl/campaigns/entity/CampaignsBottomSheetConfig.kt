package com.tangem.features.promobanners.impl.campaigns.entity

import com.tangem.domain.models.wallet.UserWalletId
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
        val userWalletId: UserWalletId,
    ) : CampaignsBottomSheetConfig()

    @Serializable
    data class AlreadyActivated(
        val campaignType: CampaignType,
    ) : CampaignsBottomSheetConfig()
}