package com.tangem.features.tangempay.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.deeplink.DeeplinkConst.SCREEN_KEY
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.pay.TangemPayDetailsInitialRoute
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

internal class DefaultTangemPayAccountDeepLinkHandler @AssistedInject constructor(
    @Assisted private val scope: CoroutineScope,
    @Assisted private val queryParams: Map<String, String>,
    private val appRouter: AppRouter,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier,
) : TangemPayAccountDeepLinkHandler {

    init {
        handleDeepLink()
    }

    private fun handleDeepLink() {
        val initialRoute = resolveInitialRoute()

        scope.launch {
            val userWallet = userWalletsListRepository.selectedUserWalletSync()
            if (userWallet == null || userWallet.isLocked) {
                TangemLogger.i("$TAG: no unlocked selected wallet")
                appRouter.popTo(AppRoute.Wallet)
                return@launch
            }

            val status = loadPaymentAccountStatus(userWallet.walletId)
            if (status == null) {
                appRouter.popTo(AppRoute.Wallet)
                return@launch
            }

            appRouter.popTo(
                route = AppRoute.Wallet,
                onComplete = {
                    appRouter.push(AppRoute.TangemPayDetails(status = status, initialRoute = initialRoute))
                },
            )
        }
    }

    private fun resolveInitialRoute(): TangemPayDetailsInitialRoute {
        val screen = queryParams[SCREEN_KEY]?.takeIf(String::isNotBlank)
            ?: return TangemPayDetailsInitialRoute.ACCOUNT_DETAILS

        return SCREEN_ROUTES[screen.lowercase()] ?: run {
            TangemLogger.i("$TAG: unknown '$SCREEN_KEY' value '$screen', falling back to the account details")
            TangemPayDetailsInitialRoute.ACCOUNT_DETAILS
        }
    }

    private suspend fun loadPaymentAccountStatus(userWalletId: UserWalletId): AccountStatus.Payment? {
        paymentAccountStatusFetcher.invoke(userWalletId)

        val status = paymentAccountStatusSupplier.invoke(userWalletId).firstOrNull()
        if (status == null || !status.value.canOpenPaymentAccount()) {
            TangemLogger.i("$TAG: selected wallet has no openable payment account")
            return null
        }

        return status
    }

    private fun PaymentAccountStatusValue.canOpenPaymentAccount(): Boolean = when (this) {
        PaymentAccountStatusValue.Empty,
        PaymentAccountStatusValue.NotCreated,
        is PaymentAccountStatusValue.Error,
        is PaymentAccountStatusValue.UnderReview,
        is PaymentAccountStatusValue.IssuingCard,
        -> false
        PaymentAccountStatusValue.Loading,
        is PaymentAccountStatusValue.AwaitingPlanSelection,
        is PaymentAccountStatusValue.Inactive,
        is PaymentAccountStatusValue.Loaded,
        is PaymentAccountStatusValue.Deactivated,
        -> true
    }

    @AssistedFactory
    interface Factory : TangemPayAccountDeepLinkHandler.Factory {
        override fun create(
            scope: CoroutineScope,
            queryParams: Map<String, String>,
        ): DefaultTangemPayAccountDeepLinkHandler
    }

    private companion object {

        const val TAG = "TangemPayAccountDeepLink"

        val SCREEN_ROUTES = mapOf(
            "add_funds" to TangemPayDetailsInitialRoute.ADD_FUNDS,
            "va_onramp" to TangemPayDetailsInitialRoute.VA_ONRAMP,
            "va_onramp_details" to TangemPayDetailsInitialRoute.VA_ONRAMP_DETAILS,
            "select_plan" to TangemPayDetailsInitialRoute.TIERS_ONBOARDING,
            "current_plan" to TangemPayDetailsInitialRoute.CURRENT_PLAN,
            "change_plan" to TangemPayDetailsInitialRoute.CHANGE_PLAN,
            "cashback" to TangemPayDetailsInitialRoute.CASHBACK,
            "order_card" to TangemPayDetailsInitialRoute.ORDER_CARD,
        )
    }
}