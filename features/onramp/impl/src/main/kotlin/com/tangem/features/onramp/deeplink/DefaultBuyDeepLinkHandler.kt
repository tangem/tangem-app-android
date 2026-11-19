package com.tangem.features.onramp.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.core.navigation.deeplink.DeepLinkHandler
import com.tangem.core.navigation.deeplink.DeepLinkRequest
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

internal class DefaultBuyDeepLinkHandler @Inject constructor(
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
) : DeepLinkHandler, BuyDeepLinkHandler {

    override fun handle(request: DeepLinkRequest) {
        getSelectedWalletSyncUseCase().fold(
            ifLeft = {
                TangemLogger.e("Error on getting user wallet: $it")
            },
            ifRight = { userWallet ->
                request.router.push(AppRoute.BuyCrypto(userWallet.walletId))
            },
        )
    }
}