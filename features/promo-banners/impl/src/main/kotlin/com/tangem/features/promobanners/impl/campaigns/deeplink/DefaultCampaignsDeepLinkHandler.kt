package com.tangem.features.promobanners.impl.campaigns.deeplink

import com.tangem.common.routing.deeplink.DeeplinkConst
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.features.promobanners.api.deeplink.CampaignsDeepLinkHandler
import com.tangem.features.promobanners.api.toggles.PromoBannersFeatureToggles
import com.tangem.features.promobanners.impl.campaigns.service.CampaignsService
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

/**
 * Handles the campaigns deeplink (`tangem://campaigns?campaignId=1&lang=ru`): extracts the `campaignId` and
 * pushes it to the promo-campaigns bus. The always-alive [com.tangem.features.promobanners.api.swapcashback
 * .SwapCashbackCampaignComponent] listens to the bus, maps the id to a campaign type, resolves the campaign
 * state and shows the right sheet over the current screen.
 */
internal class DefaultCampaignsDeepLinkHandler @AssistedInject constructor(
    @Assisted private val queryParams: Map<String, String>,
    campaignsService: CampaignsService,
    getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    promoBannersFeatureToggles: PromoBannersFeatureToggles,
) : CampaignsDeepLinkHandler {

    init {
        if (promoBannersFeatureToggles.isCampaignsToggleEnabled) {
            // It is okay here, we are navigating from outside, and there is no other way to getting UserWallet
            getSelectedWalletSyncUseCase().fold(
                ifLeft = {
                    TangemLogger.e("Error on getting user wallet")
                },
                ifRight = { userWallet ->
                    val campaignId = queryParams[DeeplinkConst.CAMPAIGN_ID_KEY].orEmpty()
                    val userWalletId = userWallet.walletId

                    campaignsService.show(campaignId = campaignId, userWalletId = userWalletId)
                },
            )
        } else {
            TangemLogger.i("Campaigns feature is disabled")
        }
    }

    @AssistedFactory
    interface Factory : CampaignsDeepLinkHandler.Factory {
        override fun create(queryParams: Map<String, String>): DefaultCampaignsDeepLinkHandler
    }
}