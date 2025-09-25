package com.tangem.features.tangempay.deeplink

import android.net.Uri
import dagger.assisted.Assisted
import com.tangem.common.routing.AppRoute
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.common.routing.AppRouter
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultOnboardVisaDeepLinkHandler @AssistedInject constructor(
    @Assisted uri: Uri,
    appRouter: AppRouter,
    tangemPayFeatureToggles: TangemPayFeatureToggles,
) : OnboardVisaDeepLinkHandler {

    init {
        if (tangemPayFeatureToggles.isTangemPayEnabled) {
            appRouter.push(AppRoute.TangemPayOnboarding(uri.toString()))
        } else {
            appRouter.push(AppRoute.Home())
        }
    }

    @AssistedFactory
    interface Factory : OnboardVisaDeepLinkHandler.Factory {
        override fun create(uri: Uri): DefaultOnboardVisaDeepLinkHandler
    }
}