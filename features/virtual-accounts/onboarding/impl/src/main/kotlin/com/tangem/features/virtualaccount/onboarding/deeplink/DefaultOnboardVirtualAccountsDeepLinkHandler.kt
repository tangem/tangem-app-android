package com.tangem.features.virtualaccount.onboarding.deeplink

import android.net.Uri
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultOnboardVirtualAccountsDeepLinkHandler @AssistedInject constructor(
    @Assisted uri: Uri,
    appRouter: AppRouter,
    userWalletsListRepository: UserWalletsListRepository,
) : OnboardVirtualAccountsDeepLinkHandler {

    init {
        val userWalletId = userWalletsListRepository.selectedUserWallet.value?.walletId
        if (userWalletId == null) {
            TangemLogger.e("Can not open virtual account onboarding deeplink: no selected wallet")
        } else {
            val mode = AppRoute.VirtualAccountOnboarding.Mode.Deeplink(
                userWalletId = userWalletId,
                deeplink = uri.toString(),
            )
            appRouter.push(AppRoute.VirtualAccountOnboarding(mode))
        }
    }

    @AssistedFactory
    interface Factory : OnboardVirtualAccountsDeepLinkHandler.Factory {
        override fun create(uri: Uri): DefaultOnboardVirtualAccountsDeepLinkHandler
    }
}