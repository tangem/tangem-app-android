package com.tangem.features.tangempay.deeplink

import android.net.Uri
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultOnboardVisaDeepLinkHandler @AssistedInject constructor(
    @Assisted uri: Uri,
    appRouter: AppRouter,
) : OnboardVisaDeepLinkHandler {

    init {
        val mode = AppRoute.TangemPayOnboarding.Mode.Deeplink(uri.toString())
        appRouter.push(AppRoute.TangemPayOnboarding(mode))
    }

    @AssistedFactory
    interface Factory : OnboardVisaDeepLinkHandler.Factory {
        override fun create(uri: Uri): DefaultOnboardVisaDeepLinkHandler
    }
}