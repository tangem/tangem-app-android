package com.tangem.feature.wallet.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.core.navigation.deeplink.DeepLinkHandler
import com.tangem.core.navigation.deeplink.DeepLinkRequest
import javax.inject.Inject

internal class WalletDeepLinkHandler @Inject constructor() : DeepLinkHandler {

    override fun handle(request: DeepLinkRequest) {
        request.router.popTo(AppRoute.Wallet)
    }

    companion object {
        const val DEEPLINK_KEY = "tangem://main"
    }
}