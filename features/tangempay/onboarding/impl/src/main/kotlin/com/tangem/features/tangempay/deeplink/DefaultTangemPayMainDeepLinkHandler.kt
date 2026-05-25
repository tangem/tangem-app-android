package com.tangem.features.tangempay.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.deeplink.DeeplinkConst.CUSTOMER_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.CUSTOMER_WALLET_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TYPE_KEY
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.visa.model.TangemPayPushNotificationType
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.SelectWalletUseCase
import com.tangem.features.wallet.deeplink.WalletDeepLinkActionTrigger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
internal class DefaultTangemPayMainDeepLinkHandler @AssistedInject constructor(
    @Assisted private val scope: CoroutineScope,
    @Assisted private val payload: Map<String, String>,
    private val appRouter: AppRouter,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val selectWalletUseCase: SelectWalletUseCase,
    private val walletDeepLinkActionTrigger: WalletDeepLinkActionTrigger,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
    private val paymentAccountSupplier: PaymentAccountStatusSupplier,
) : TangemPayMainDeepLinkHandler {

    init {
        handleDeepLink()
    }

    private fun handleDeepLink() {
        val walletId = payload[CUSTOMER_WALLET_ID_KEY]

        scope.launch {
            val userWalletId = walletId?.let(::UserWalletId) ?: run {
                appRouter.popTo(AppRoute.Wallet)
                return@launch
            }
            val userWallet = getUserWalletUseCase(userWalletId).getOrNull()
            if (userWallet == null || userWallet.isLocked) {
                appRouter.popTo(AppRoute.Wallet)
                return@launch
            }
            if (selectWalletUseCase(userWalletId).getOrNull() == null) {
                appRouter.popTo(AppRoute.Wallet)
                return@launch
            }

            val pushAction = buildPushAction()

            appRouter.popTo(
                route = AppRoute.Wallet,
                onComplete = {
                    walletDeepLinkActionTrigger.selectWallet(userWalletId)
                    when (pushAction) {
                        is TangemPayPushAction.CardReady,
                        is TangemPayPushAction.TopUp,
                        -> navigateToTangemPayDetails(userWalletId)
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
                        null -> Unit
                    }
                },
            )
        }
    }

    private fun buildPushAction(): TangemPayPushAction? {
        val type = payload[TYPE_KEY]?.let(TangemPayPushNotificationType::fromValue) ?: return null
        val customerId = payload[CUSTOMER_ID_KEY].orEmpty()

        return when (type) {
            TangemPayPushNotificationType.CARD_READY -> TangemPayPushAction.CardReady
            TangemPayPushNotificationType.TRANSACTION_SPEND -> {
                val transaction = TangemPayPushPayloadToTxHistoryItemConverter.convertSpend(payload)
                if (transaction != null) TangemPayPushAction.TransactionSpend(transaction, customerId) else null
            }
            TangemPayPushNotificationType.TOP_UP -> TangemPayPushAction.TopUp
            TangemPayPushNotificationType.COLLATERAL -> {
                val transaction = TangemPayPushPayloadToTxHistoryItemConverter.convertCollateral(payload)
                if (transaction != null) TangemPayPushAction.CollateralTransaction(transaction, customerId) else null
            }
        }
    }

    private fun navigateToTangemPayDetails(walletId: UserWalletId) {
        scope.launch {
            paymentAccountStatusFetcher.invoke(PaymentAccountStatusFetcher.Params(walletId))
            val paymentAccountStatus = paymentAccountSupplier.invoke(userWalletId = walletId)
                .firstOrNull()
                ?: return@launch
            appRouter.push(route = AppRoute.TangemPayDetails(status = paymentAccountStatus))
        }
    }

    @AssistedFactory
    interface Factory : TangemPayMainDeepLinkHandler.Factory {
        override fun create(scope: CoroutineScope, payload: Map<String, String>): DefaultTangemPayMainDeepLinkHandler
    }
}