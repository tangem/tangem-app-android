package com.tangem.features.moneyaccount.onboarding.deeplink

import android.net.Uri
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultOnboardMoneyAccountDeepLinkHandler @AssistedInject constructor(
    @Assisted uri: Uri,
    appRouter: AppRouter,
    userWalletsListRepository: UserWalletsListRepository,
) : OnboardMoneyAccountDeepLinkHandler {

    init {
        val userWalletId = userWalletsListRepository.selectedUserWallet.value?.walletId
        if (userWalletId == null) {
            TangemLogger.e("Can not open money account onboarding deeplink: no selected wallet")
        } else {
            val mode = AppRoute.MoneyAccountOnboarding.Mode.Deeplink(
                userWalletId = userWalletId,
                deeplink = uri.toString(),
            )
            appRouter.push(AppRoute.MoneyAccountOnboarding(mode))
        }
    }

    @AssistedFactory
    interface Factory : OnboardMoneyAccountDeepLinkHandler.Factory {
        override fun create(uri: Uri): DefaultOnboardMoneyAccountDeepLinkHandler
    }
}