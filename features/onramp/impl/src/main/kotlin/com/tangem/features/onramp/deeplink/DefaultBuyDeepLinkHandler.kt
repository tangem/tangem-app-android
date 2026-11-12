package com.tangem.features.onramp.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

internal class DefaultBuyDeepLinkHandler @Inject constructor(
    private val router: AppRouter,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
) : DeepLinkHandler, BuyDeepLinkHandler {

    override fun handle(request: DeepLinkRequest) {
        // It is okay here, we are navigating from outside, and there is no other way to getting UserWallet
        getSelectedWalletSyncUseCase().fold(
            ifLeft = {
                TangemLogger.e("Error on getting user wallet: $it")
            },
            ifRight = { userWallet ->
                router.push(AppRoute.BuyCrypto(userWallet.walletId))
            },
        )
    }
}