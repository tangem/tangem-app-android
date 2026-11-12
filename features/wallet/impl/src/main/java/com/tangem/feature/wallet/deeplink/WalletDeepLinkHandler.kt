package com.tangem.feature.wallet.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkRequest
import javax.inject.Inject

internal class WalletDeepLinkHandler @Inject constructor(
    private val router: AppRouter,
) : DeepLinkHandler {

    override fun handle(request: DeepLinkRequest) {
        router.popTo(AppRoute.Wallet)
    }

    companion object {
        const val DEEPLINK_KEY = "tangem://main"
    }
}