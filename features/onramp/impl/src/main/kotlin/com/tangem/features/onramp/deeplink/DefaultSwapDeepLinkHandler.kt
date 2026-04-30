package com.tangem.features.onramp.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultSwapDeepLinkHandler @AssistedInject constructor(
    router: AppRouter,
    getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
) : SwapDeepLinkHandler {

    init {
        // It is okay here, we are navigating from outside, and there is no other way to getting UserWallet
        getSelectedWalletSyncUseCase().fold(
            ifLeft = {
                TangemLogger.e("Error on getting user wallet: $it")
            },
            ifRight = { userWallet ->
                router.push(
                    AppRoute.Swap(
                        userWalletId = userWallet.walletId,
                        screenSource = AnalyticsParam.ScreensSources.Main.value,
                    ),
                )
            },
        )
    }

    @AssistedFactory
    interface Factory : SwapDeepLinkHandler.Factory {
        override fun create(): DefaultSwapDeepLinkHandler
    }
}