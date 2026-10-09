package com.tangem.features.onramp.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import com.tangem.utils.logging.TangemLogger

internal class DefaultBuyDeepLinkHandler @AssistedInject constructor(
    router: AppRouter,
    getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
) : BuyDeepLinkHandler {

    init {
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

    @AssistedFactory
    interface Factory : BuyDeepLinkHandler.Factory {
        override fun create(): DefaultBuyDeepLinkHandler
    }

    companion object {
        const val DEEPLINK_KEY = "tangem://buy"

        /** Return link of the onramp provider after a purchase: it only brings the app back, nothing to open. */
        const val REDIRECT_DEEPLINK_KEY = "tangem://redirect"
    }
}