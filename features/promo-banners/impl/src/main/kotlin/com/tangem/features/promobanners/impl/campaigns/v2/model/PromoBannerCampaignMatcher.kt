package com.tangem.features.promobanners.impl.campaigns.v2.model

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.promo.models.PromoCampaign
import com.tangem.features.promobanners.impl.model.PromoBannerDisplay
import javax.inject.Inject

/** Campaigns of one wallet as seen by the banner block. A wallet without an entry is still loading. */
internal sealed interface WalletCampaigns {

    data object Failed : WalletCampaigns

    data class Loaded(val userWallet: UserWallet, val campaigns: List<PromoCampaign>) : WalletCampaigns
}

/**
 * A banner linked to a campaign is shown only while that campaign is open for the wallet: tapping it must lead to
 * the campaign, not to the "not found" or "already enrolled" sheet. Banners without a campaign are not affected.
 */
internal class PromoBannerCampaignMatcher @Inject constructor() {

    fun filter(banners: List<PromoBannerDisplay>, walletCampaigns: WalletCampaigns?): List<PromoBannerDisplay> {
        return banners.filter { banner -> isShown(banner, walletCampaigns) }
    }

    private fun isShown(banner: PromoBannerDisplay, walletCampaigns: WalletCampaigns?): Boolean {
        val campaignId = banner.campaignId ?: return true
        if (walletCampaigns !is WalletCampaigns.Loaded) return false

        val campaign = walletCampaigns.campaigns.firstOrNull { it.id == campaignId } ?: return false

        return !campaign.isAlreadyEnrolled
    }
}