package com.tangem.feature.wallet.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.features.wallet.deeplink.WalletDeepLinkHandler
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultWalletDeepLinkHandler @AssistedInject constructor(
    router: AppRouter,
) : WalletDeepLinkHandler {

    init {
        router.popTo(AppRoute.Wallet)
    }

    @AssistedFactory
    interface Factory : WalletDeepLinkHandler.Factory {
        override fun create(): DefaultWalletDeepLinkHandler
    }
}