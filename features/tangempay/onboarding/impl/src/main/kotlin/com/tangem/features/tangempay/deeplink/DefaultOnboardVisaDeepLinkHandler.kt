package com.tangem.features.tangempay.deeplink

import android.net.Uri
import dagger.assisted.Assisted
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultOnboardVisaDeepLinkHandler @AssistedInject constructor(
    @Assisted uri: Uri,
    appRouter: AppRouter,
) : OnboardVisaDeepLinkHandler {

    init {
        appRouter.push(AppRoute.TangemPayOnboarding(uri.toString()))
    }

    @AssistedFactory
    interface Factory : OnboardVisaDeepLinkHandler.Factory {
        override fun create(uri: Uri): DefaultOnboardVisaDeepLinkHandler
    }
}