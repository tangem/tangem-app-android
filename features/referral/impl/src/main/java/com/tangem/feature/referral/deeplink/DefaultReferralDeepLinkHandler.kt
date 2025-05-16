package com.tangem.feature.referral.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.feature.referral.api.deeplink.ReferralDeepLinkHandler
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import timber.log.Timber

internal class DefaultReferralDeepLinkHandler @AssistedInject constructor(
    appRouter: AppRouter,
    getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
) : ReferralDeepLinkHandler {

    init {
        // It is okay here, we are navigating from outside, and there is no other way to getting UserWallet
        getSelectedWalletSyncUseCase().fold(
            ifLeft = {
                Timber.e("Error on getting user wallet: $it")
            },
            ifRight = { userWallet ->
                if (userWallet.cardTypesResolver.isTangemWallet()) {
                    appRouter.push(
                        AppRoute.ReferralProgram(userWalletId = userWallet.walletId),
                    )
                }
            },
        )
    }

    @AssistedFactory
    interface Factory : ReferralDeepLinkHandler.Factory {
        override fun create(): DefaultReferralDeepLinkHandler
    }
}