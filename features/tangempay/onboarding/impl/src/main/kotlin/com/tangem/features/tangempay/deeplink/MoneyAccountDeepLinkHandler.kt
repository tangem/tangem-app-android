package com.tangem.features.tangempay.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.deeplink.DeepLinkHandler
import com.tangem.core.navigation.deeplink.DeepLinkRequest
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusFetcher
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusSupplier
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class MoneyAccountDeepLinkHandler @Inject constructor(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val moneyAccountStatusFetcher: MoneyAccountStatusFetcher,
    private val moneyAccountStatusSupplier: MoneyAccountStatusSupplier,
    private val onboardDeepLinkHandler: DefaultTangemPayOnboardDeepLinkHandler,
) : DeepLinkHandler {

    override fun handle(request: DeepLinkRequest) {
        request.scope.launch {
            val userWallet = userWalletsListRepository.selectedUserWalletSync()
            if (userWallet == null || userWallet.isLocked) {
                onboardDeepLinkHandler.handle(request)
                return@launch
            }

            when (loadStatus(userWallet.walletId)) {
                is MoneyAccountStatusValue.Account -> openOnTopOfWallet(
                    router = request.router,
                    route = AppRoute.MoneyAccountDetails(userWalletId = userWallet.walletId),
                )
                is MoneyAccountStatusValue.Onboarding -> {
                    TangemLogger.i("$TAG: activation in progress, returning to the wallet")
                    request.router.popTo(AppRoute.Wallet)
                }
                MoneyAccountStatusValue.Empty,
                is MoneyAccountStatusValue.Error,
                null,
                -> onboardDeepLinkHandler.handle(request)
            }
        }
    }

    private suspend fun loadStatus(userWalletId: UserWalletId): MoneyAccountStatusValue? {
        moneyAccountStatusFetcher(userWalletId)
        return moneyAccountStatusSupplier(userWalletId).firstOrNull()?.value
    }

    private fun openOnTopOfWallet(router: Router, route: AppRoute) {
        router.popTo(
            route = AppRoute.Wallet,
            onComplete = { router.push(route) },
        )
    }

    companion object {

        const val DEEPLINK_KEY = "tangem://onboard-virtual-account"

        private const val TAG = "MoneyAccountDeepLink"
    }
}