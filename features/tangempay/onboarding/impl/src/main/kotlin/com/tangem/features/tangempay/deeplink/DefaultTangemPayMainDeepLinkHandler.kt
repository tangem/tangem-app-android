package com.tangem.features.tangempay.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.deeplink.DeeplinkConst.CUSTOMER_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.CUSTOMER_WALLET_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TYPE_KEY
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.deeplink.DeepLinkHandler
import com.tangem.core.navigation.deeplink.DeepLinkRequest
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.tangempay.TangemPayDetailsInitialRoute
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusFetcher
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusSupplier
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoPushOpenedEvent
import com.tangem.domain.tangempay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.tangempay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.tangempay.model.TangemPayPushNotificationType
import com.tangem.domain.tangempay.model.action
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.SelectWalletUseCase
import com.tangem.features.wallet.deeplink.WalletDeepLinkActionTrigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
internal class DefaultTangemPayMainDeepLinkHandler @Inject constructor(
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val selectWalletUseCase: SelectWalletUseCase,
    private val walletDeepLinkActionTrigger: WalletDeepLinkActionTrigger,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
    private val paymentAccountSupplier: PaymentAccountStatusSupplier,
    private val moneyAccountStatusFetcher: MoneyAccountStatusFetcher,
    private val moneyAccountStatusSupplier: MoneyAccountStatusSupplier,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : DeepLinkHandler, TangemPayMainDeepLinkHandler {

    override fun handle(request: DeepLinkRequest) {
        val scope = request.scope
        val router = request.router
        val payload = request.queryParams
        val walletId = payload[CUSTOMER_WALLET_ID_KEY]
        val pushType = payload[TYPE_KEY]?.let(TangemPayPushNotificationType::fromValue)

        if (pushType == TangemPayPushNotificationType.KYC_ADDITIONAL_INFO) {
            analyticsEventHandler.send(TangemPayKycAdditionalInfoPushOpenedEvent())
        }

        scope.launch {
            val userWalletId = walletId?.let(UserWalletId::fromHexOrNull) ?: run {
                router.popTo(AppRoute.Wallet)
                return@launch
            }
            val userWallet = getUserWalletUseCase(userWalletId).getOrNull()
            if (userWallet == null || userWallet.isLocked) {
                router.popTo(AppRoute.Wallet)
                return@launch
            }
            if (selectWalletUseCase(userWalletId).getOrNull() == null) {
                router.popTo(AppRoute.Wallet)
                return@launch
            }

            val pushAction = pushType?.let { buildPushAction(type = it, payload = payload) }

            router.popTo(
                route = AppRoute.Wallet,
                onComplete = {
                    walletDeepLinkActionTrigger.selectWallet(userWalletId)
                    when (pushAction) {
                        is TangemPayPushAction.CardReady -> navigateToTangemPayDetails(
                            scope = scope,
                            router = router,
                            walletId = userWalletId,
                            initialRoute = TangemPayDetailsInitialRoute.ACCOUNT_DETAILS,
                        )
                        is TangemPayPushAction.TopUp -> navigateToTangemPayDetails(
                            scope = scope,
                            router = router,
                            walletId = userWalletId,
                            initialRoute = TangemPayDetailsInitialRoute.ADD_FUNDS,
                        )
                        is TangemPayPushAction.TransactionSpend -> {
                            walletDeepLinkActionTrigger.showTangemPayTransaction(
                                transaction = pushAction.transaction,
                                customerId = pushAction.customerId,
                            )
                        }
                        is TangemPayPushAction.CollateralTransaction -> {
                            walletDeepLinkActionTrigger.showTangemPayTransaction(
                                transaction = pushAction.transaction,
                                customerId = pushAction.customerId,
                            )
                        }
                        is TangemPayPushAction.KycAdditionalInfo -> openKycAdditionalInfoAccount(
                            scope = scope,
                            router = router,
                            walletId = userWalletId,
                        )
                        null -> Unit
                    }
                },
            )
        }
    }

    private fun buildPushAction(
        type: TangemPayPushNotificationType,
        payload: Map<String, String>,
    ): TangemPayPushAction? {
        val customerId = payload[CUSTOMER_ID_KEY].orEmpty()

        return when (type.action()) {
            TangemPayPushNotificationType.Action.CARD_DETAILS -> TangemPayPushAction.CardReady
            TangemPayPushNotificationType.Action.SPEND_DETAILS -> {
                val transaction = TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(payload)
                if (transaction != null) TangemPayPushAction.TransactionSpend(transaction, customerId) else null
            }
            TangemPayPushNotificationType.Action.COLLATERAL_DETAILS -> {
                val transaction = TangemPayPushPayloadToTxHistoryItemConverter.convertCollateral(payload)
                if (transaction != null) TangemPayPushAction.CollateralTransaction(transaction, customerId) else null
            }
            TangemPayPushNotificationType.Action.TOP_UP -> TangemPayPushAction.TopUp
            TangemPayPushNotificationType.Action.KYC_ADDITIONAL_INFO -> TangemPayPushAction.KycAdditionalInfo
        }
    }

    private fun navigateToTangemPayDetails(
        scope: CoroutineScope,
        router: Router,
        walletId: UserWalletId,
        initialRoute: TangemPayDetailsInitialRoute,
    ) {
        scope.launch {
            paymentAccountStatusFetcher.invoke(PaymentAccountStatusFetcher.Params(walletId))
            val paymentAccountStatus = paymentAccountSupplier.invoke(userWalletId = walletId)
                .firstOrNull()
                ?: return@launch
            router.push(
                route = AppRoute.TangemPayDetails(
                    status = paymentAccountStatus,
                    initialRoute = initialRoute,
                ),
            )
        }
    }

    private fun openKycAdditionalInfoAccount(scope: CoroutineScope, router: Router, walletId: UserWalletId) {
        scope.launch {
            val route = resolveKycAdditionalInfoRoute(walletId) ?: return@launch
            router.push(route)
        }
    }

    private suspend fun resolveKycAdditionalInfoRoute(walletId: UserWalletId): AppRoute? {
        paymentAccountStatusFetcher(PaymentAccountStatusFetcher.Params(walletId))
        val payment = paymentAccountSupplier(userWalletId = walletId).firstOrNull()
        val loadedPayment = payment?.value as? PaymentAccountStatusValue.Loaded
        val paymentRoute = payment?.let { AppRoute.TangemPayDetails(status = it, isOpenedFromKycPush = true) }

        return when {
            loadedPayment?.isPaymentAccountIssuing == false -> paymentRoute
            refreshAndCheckMoneyAccount(walletId) -> AppRoute.MoneyAccountDetails(
                userWalletId = walletId,
                isOpenedFromKycPush = true,
            )
            loadedPayment != null -> paymentRoute
            else -> null
        }
    }

    private suspend fun refreshAndCheckMoneyAccount(walletId: UserWalletId): Boolean {
        moneyAccountStatusFetcher(MoneyAccountStatusFetcher.Params(walletId))
        return moneyAccountStatusSupplier(walletId).firstOrNull()?.value is MoneyAccountStatusValue.Account
    }
}