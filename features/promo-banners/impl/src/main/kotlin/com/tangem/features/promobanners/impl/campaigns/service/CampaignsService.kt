package com.tangem.features.promobanners.impl.campaigns.service

import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

/**
 * App-wide bus that decouples the promo-campaigns deeplink handler from the UI that shows the campaign
 * bottom sheet. A producer (deeplink handler) calls [show]; the always-alive campaign component listens
 * to [campaignFlow] and activates the appropriate sheet over the current screen.
 */
internal interface CampaignsService {

    /** Emits the campaign requested via [show]. */
    val campaignFlow: Flow<CampaignRequest>

    /** Requests showing the campaign identified by [campaignId] for the given [userWalletId]. */
    fun show(campaignId: String, userWalletId: UserWalletId)
}

/** Payload of the campaigns bus: the campaign id and the wallet the campaign should be activated for. */
internal data class CampaignRequest(
    val campaignId: String,
    val userWalletId: UserWalletId,
)