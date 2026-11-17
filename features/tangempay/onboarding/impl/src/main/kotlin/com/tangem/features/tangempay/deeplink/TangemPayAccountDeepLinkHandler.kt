package com.tangem.features.tangempay.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.deeplink.DeeplinkConst.SCREEN_KEY
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.deeplink.DeepLinkHandler
import com.tangem.core.navigation.deeplink.DeepLinkRequest
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.tangempay.TangemPayDetailsInitialRoute
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.tangempay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.tangempay.flow.PaymentAccountStatusSupplier
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class TangemPayAccountDeepLinkHandler @Inject constructor(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier,
) : DeepLinkHandler {

    override fun handle(request: DeepLinkRequest) {
        val router = request.router
        val screen = request.queryParams[SCREEN_KEY]?.takeIf(String::isNotBlank)?.lowercase()

        request.scope.launch {
            val userWallet = userWalletsListRepository.selectedUserWalletSync()
            if (userWallet == null || userWallet.isLocked) {
                TangemLogger.i("$TAG: no unlocked selected wallet")
                router.popTo(AppRoute.Wallet)
                return@launch
            }

            val status = loadPaymentAccountStatus(userWallet.walletId)
            when {
                status == null -> router.popTo(AppRoute.Wallet)
                screen == SELECT_PLAN_SCREEN && status.value is PaymentAccountStatusValue.Verified -> {
                    openOnTopOfWallet(router, AppRoute.TangemPayVerified(userWalletId = userWallet.walletId))
                }
                status.value.canOpenPaymentAccount() -> {
                    val initialRoute = resolveInitialRoute(screen)
                    openOnTopOfWallet(router, AppRoute.TangemPayDetails(status = status, initialRoute = initialRoute))
                }
                else -> {
                    TangemLogger.i("$TAG: selected wallet has no openable payment account")
                    router.popTo(AppRoute.Wallet)
                }
            }
        }
    }

    private fun openOnTopOfWallet(router: Router, route: AppRoute) {
        router.popTo(
            route = AppRoute.Wallet,
            onComplete = { router.push(route) },
        )
    }

    private fun resolveInitialRoute(screen: String?): TangemPayDetailsInitialRoute {
        if (screen == null || screen == SELECT_PLAN_SCREEN) return TangemPayDetailsInitialRoute.ACCOUNT_DETAILS

        return SCREEN_ROUTES[screen] ?: run {
            TangemLogger.i("$TAG: unknown '$SCREEN_KEY' value '$screen', falling back to the account details")
            TangemPayDetailsInitialRoute.ACCOUNT_DETAILS
        }
    }

    private suspend fun loadPaymentAccountStatus(userWalletId: UserWalletId): AccountStatus.Payment? {
        paymentAccountStatusFetcher.invoke(userWalletId)
        return paymentAccountStatusSupplier.invoke(userWalletId).firstOrNull()
    }

    private fun PaymentAccountStatusValue.canOpenPaymentAccount(): Boolean = when (this) {
        PaymentAccountStatusValue.Empty,
        PaymentAccountStatusValue.NotCreated,
        is PaymentAccountStatusValue.Error,
        is PaymentAccountStatusValue.KycRequired,
        is PaymentAccountStatusValue.UnderReview,
        is PaymentAccountStatusValue.Verified,
        -> false
        PaymentAccountStatusValue.Loading,
        is PaymentAccountStatusValue.Loaded,
        is PaymentAccountStatusValue.Deactivated,
        -> true
    }

    companion object {

        const val DEEPLINK_KEY = "tangem://pay-account"

        private const val TAG = "TangemPayAccountDeepLink"
        private const val SELECT_PLAN_SCREEN = "select_plan"

        private val SCREEN_ROUTES = mapOf(
            "add_funds" to TangemPayDetailsInitialRoute.ADD_FUNDS,
            "va_onramp" to TangemPayDetailsInitialRoute.VA_ONRAMP,
            "va_onramp_details" to TangemPayDetailsInitialRoute.VA_ONRAMP_DETAILS,
            "current_plan" to TangemPayDetailsInitialRoute.CURRENT_PLAN,
            "change_plan" to TangemPayDetailsInitialRoute.CHANGE_PLAN,
            "cashback" to TangemPayDetailsInitialRoute.CASHBACK,
            "order_card" to TangemPayDetailsInitialRoute.ORDER_CARD,
        )
    }
}