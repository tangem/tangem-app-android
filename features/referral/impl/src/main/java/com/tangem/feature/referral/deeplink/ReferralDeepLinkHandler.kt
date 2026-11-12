package com.tangem.feature.referral.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

internal class ReferralDeepLinkHandler @Inject constructor(
    private val appRouter: AppRouter,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
) : DeepLinkHandler {

    override fun handle(request: DeepLinkRequest) {
        // It is okay here, we are navigating from outside, and there is no other way to getting UserWallet
        getSelectedWalletSyncUseCase().fold(
            ifLeft = {
                TangemLogger.e("Error on getting user wallet: $it")
            },
            ifRight = { userWallet ->
                if (userWallet !is UserWallet.Cold || userWallet.cardTypesResolver.isTangemWallet()) {
                    appRouter.push(
                        AppRoute.ReferralProgram(userWalletId = userWallet.walletId),
                    )
                }
            },
        )
    }

    companion object {
        const val DEEPLINK_KEY = "tangem://referral"
    }
}