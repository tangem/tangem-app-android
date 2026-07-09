package com.tangem.features.promobanners.impl.campaigns.service

import kotlinx.coroutines.flow.Flow

/**
 * App-wide bus that decouples the promo-campaigns deeplink handler from the UI that shows the campaign
 * bottom sheet. A producer (deeplink handler) calls [show]; the always-alive campaign component listens
 * to [campaignFlow] and activates the appropriate sheet over the current screen.
 */
internal interface CampaignsService {

    /** Emits the campaignId requested via [show]. */
    val campaignFlow: Flow<String>

    /** Requests showing the campaign identified by [campaignId]. */
    fun show(campaignId: String)
}