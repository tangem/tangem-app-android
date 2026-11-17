package com.tangem.features.tangempay.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.core.navigation.deeplink.DeepLinkRequest
import javax.inject.Inject

internal class DefaultTangemPayOnboardDeepLinkHandler @Inject constructor() : TangemPayOnboardDeepLinkHandler {

    override fun handle(request: DeepLinkRequest) {
        val mode = AppRoute.TangemPayOnboarding.Mode.Deeplink(
            deeplink = request.uri.toString(),
        )
        request.router.push(AppRoute.TangemPayOnboarding(mode))
    }
}