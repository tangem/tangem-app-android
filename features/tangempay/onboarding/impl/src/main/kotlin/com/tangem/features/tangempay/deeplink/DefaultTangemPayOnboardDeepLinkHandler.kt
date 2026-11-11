package com.tangem.features.tangempay.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.deeplink.DeepLinkRequest
import javax.inject.Inject

internal class DefaultTangemPayOnboardDeepLinkHandler @Inject constructor(
    private val appRouter: AppRouter,
) : TangemPayOnboardDeepLinkHandler {

    override fun handle(request: DeepLinkRequest) {
        val mode = AppRoute.TangemPayOnboarding.Mode.Deeplink(
            deeplink = request.uri.toString(),
        )
        appRouter.push(AppRoute.TangemPayOnboarding(mode))
    }
}