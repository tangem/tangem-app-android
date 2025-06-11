package com.tangem.features.onramp.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import timber.log.Timber

internal class DefaultBuyDeepLinkHandler @AssistedInject constructor(
    router: AppRouter,
    getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
) : BuyDeepLinkHandler {

    init {
        // It is okay here, we are navigating from outside, and there is no other way to getting UserWallet
        getSelectedWalletSyncUseCase().fold(
            ifLeft = {
                Timber.e("Error on getting user wallet: $it")
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
}