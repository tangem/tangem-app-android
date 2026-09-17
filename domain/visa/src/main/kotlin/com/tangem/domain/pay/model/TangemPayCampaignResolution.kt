package com.tangem.domain.pay.model

import com.tangem.domain.models.wallet.UserWalletId

sealed interface TangemPayCampaignResolution {

    data class IssueCard(
        val userWalletId: UserWalletId,
        val campaign: TangemPayCampaign,
        val offer: Offer,
    ) : TangemPayCampaignResolution

    data class Landing(val campaign: TangemPayCampaign) : TangemPayCampaignResolution

    data class SessionExpired(val userWalletId: UserWalletId) : TangemPayCampaignResolution

    data object Unavailable : TangemPayCampaignResolution
}