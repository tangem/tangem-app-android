package com.tangem.features.tangempay.deeplink

import android.net.Uri
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.features.tangempay.TangemPayFeatureToggles
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultOnboardVisaDeepLinkHandler @AssistedInject constructor(
    @Assisted uri: Uri,
    appRouter: AppRouter,
    tangemPayFeatureToggles: TangemPayFeatureToggles,
    getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
) : OnboardVisaDeepLinkHandler {

    init {
        if (tangemPayFeatureToggles.isTangemPayEnabled) {
            val userWallet = getSelectedWalletSyncUseCase.invoke().getOrNull()
            val mode = AppRoute.TangemPayOnboarding.Mode.Deeplink(
                deeplink = uri.toString(),
                userWalletId = userWallet?.walletId,
            )
            appRouter.push(AppRoute.TangemPayOnboarding(mode))
        } else {
            appRouter.push(AppRoute.Home())
        }
    }

    @AssistedFactory
    interface Factory : OnboardVisaDeepLinkHandler.Factory {
        override fun create(uri: Uri): DefaultOnboardVisaDeepLinkHandler
    }
}