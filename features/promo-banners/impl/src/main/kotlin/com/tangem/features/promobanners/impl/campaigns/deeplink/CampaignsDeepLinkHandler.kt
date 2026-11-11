package com.tangem.features.promobanners.impl.campaigns.deeplink

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.common.routing.deeplink.DeeplinkConst
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.features.promobanners.impl.campaigns.service.CampaignsService
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

/**
 * Handles the campaigns deeplink (`tangem://campaigns?campaignId=1&lang=ru`): extracts the `campaignId` and
 * pushes it to the promo-campaigns bus. The always-alive [com.tangem.features.promobanners.api.swapcashback
 * .SwapCashbackCampaignComponent] listens to the bus, maps the id to a campaign type, resolves the campaign
 * state and shows the right sheet over the current screen.
 */
internal class CampaignsDeepLinkHandler @Inject constructor(
    private val campaignsService: CampaignsService,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
) : DeepLinkHandler {

    override fun handle(request: DeepLinkRequest) {
        // It is okay here, we are navigating from outside, and there is no other way to getting UserWallet
        getSelectedWalletSyncUseCase().fold(
            ifLeft = {
                TangemLogger.e("Error on getting user wallet")
            },
            ifRight = { userWallet ->
                val campaignId = request.queryParams[DeeplinkConst.CAMPAIGN_ID_KEY].orEmpty()
                val userWalletId = userWallet.walletId

                campaignsService.show(campaignId = campaignId, userWalletId = userWalletId)
            },
        )
    }

    companion object {
        const val DEEPLINK_KEY = "tangem://campaigns"
    }
}