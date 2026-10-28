package com.tangem.features.promobanners.impl.campaigns.entity

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.promo.models.PromoCampaign

internal sealed interface CampaignsBottomSheetConfig {

    data class Campaign(
        val campaign: PromoCampaign,
        val userWalletId: UserWalletId,
    ) : CampaignsBottomSheetConfig

    data object NotFound : CampaignsBottomSheetConfig

    data class AlreadyEnrolled(
        val campaign: PromoCampaign,
    ) : CampaignsBottomSheetConfig
}