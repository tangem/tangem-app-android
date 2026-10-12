package com.tangem.features.promobanners.impl.campaigns.v2.model

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.promo.models.PromoCampaign
import com.tangem.domain.promo.models.PromoEnrollment
import com.tangem.domain.promo.models.PromoMatchMode
import com.tangem.domain.promo.models.PromoWalletType

/** [PromoMatchMode.LinkedWallet] registers every wallet on the device, all sharing one [payout]. */
internal object PromoEnrollmentsFactory {

    fun create(
        campaign: PromoCampaign,
        mainWalletId: UserWalletId,
        wallets: List<UserWallet>,
        payout: PromoEnrollment.PromoPayoutTarget?,
    ): List<PromoEnrollment> {
        val enrolledWallets = when (campaign.matchMode) {
            PromoMatchMode.Strict -> wallets.filter { it.walletId == mainWalletId }
            PromoMatchMode.LinkedWallet -> wallets
        }
        val campaignPayout = payout.takeIf { campaign.payout.isNeeded }

        return enrolledWallets.map { wallet ->
            PromoEnrollment(
                userWalletId = wallet.walletId,
                walletType = when (wallet) {
                    is UserWallet.Cold -> PromoWalletType.Card
                    is UserWallet.Hot -> PromoWalletType.Mobile
                },
                batchId = (wallet as? UserWallet.Cold)?.scanResponse?.card?.batchId,
                payout = campaignPayout,
                isMain = wallet.walletId == mainWalletId,
            )
        }
    }
}