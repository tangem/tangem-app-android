package com.tangem.features.promobanners.impl.campaigns.entity

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.promo.models.PromoCampaign

internal sealed interface CampaignsBottomSheetConfig {

    sealed interface V1 : CampaignsBottomSheetConfig {

        data object NotActive : V1

        data class Enrolled(
            val campaignType: CampaignType,
        ) : V1

        data class Activate(
            val campaignType: CampaignType,
            val userWalletId: UserWalletId,
        ) : V1

        data class AlreadyActivated(
            val campaignType: CampaignType,
        ) : V1
    }

    sealed interface V2 : CampaignsBottomSheetConfig {

        data class Campaign(
            val campaign: PromoCampaign,
            val userWalletId: UserWalletId,
        ) : V2

        data object NotFound : V2

        data class AlreadyEnrolled(
            val campaign: PromoCampaign,
        ) : V2
    }
}