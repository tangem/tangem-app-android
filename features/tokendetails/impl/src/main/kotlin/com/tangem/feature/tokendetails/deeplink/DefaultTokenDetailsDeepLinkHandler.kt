package com.tangem.feature.tokendetails.deeplink

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.deeplink.DeeplinkConst.DERIVATION_PATH_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.NETWORK_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TOKEN_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TRANSACTION_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TYPE_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.WALLET_ID_KEY
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.notifications.models.NotificationType
import com.tangem.domain.account.status.utils.CryptoCurrencyBalanceFetcher
import com.tangem.domain.tokens.wallet.WalletBalanceFetcher
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.SelectWalletUseCase
import com.tangem.features.pushnotifications.api.analytics.PushNotificationAnalyticEvents
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.features.tokendetails.deeplink.TokenDetailsDeepLinkHandler
import com.tangem.features.wallet.deeplink.WalletDeepLinkActionTrigger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.tangem.utils.logging.TangemLogger

@Suppress("LongParameterList")
internal class DefaultTokenDetailsDeepLinkHandler @AssistedInject constructor(
    @Assisted private val scope: CoroutineScope,
    @Assisted private val queryParams: Map<String, String>,
    @Assisted private val isFromOnNewIntent: Boolean,
    private val appRouter: AppRouter,
    private val selectWalletUseCase: SelectWalletUseCase,
    private val cryptoCurrencyBalanceFetcher: CryptoCurrencyBalanceFetcher,
    private val tokenDetailsDeepLinkActionTrigger: TokenDetailsDeepLinkActionTrigger,
    private val walletDeepLinkActionTrigger: WalletDeepLinkActionTrigger,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val walletBalanceFetcher: WalletBalanceFetcher,
    private val tangemPayFeatureToggles: TangemPayFeatureToggles,
    private val singleAccountListSupplier: SingleAccountListSupplier,
) : TokenDetailsDeepLinkHandler {

    init {
        handleDeepLink()
    }

    private fun handleDeepLink() {
        val networkId = queryParams[NETWORK_ID_KEY]
        val tokenId = queryParams[TOKEN_ID_KEY]
        val type = NotificationType.getType(queryParams[TYPE_KEY])
        val transactionId = queryParams[TRANSACTION_ID_KEY]
        val walletId = queryParams[WALLET_ID_KEY]

        scope.launch {
            val userWalletId = walletId?.let(::UserWalletId)
            val selectedUserWallet = getSelectedWalletSyncUseCase().getOrNull()
            val userWallet = if (userWalletId != null) {
                getUserWalletUseCase(userWalletId).getOrNull()
            } else {
                selectedUserWallet
            }
            // If wallet to select is null or locked, ignore deeplink
            if (userWallet == null || userWallet.isLocked) {
                TangemLogger.e("Error on getting user wallet")
                return@launch
            }
            if (userWalletId != null && selectedUserWallet?.walletId != userWalletId) {
                val isSelectionFailed = selectWalletUseCase(userWalletId).getOrNull() == null
                if (isSelectionFailed) {
                    TangemLogger.e("Error on selecting user wallet")
                    return@launch
                }
            }

            val cryptoCurrency = findCryptoCurrency(userWallet = userWallet, networkId = networkId, tokenId = tokenId)

            if (cryptoCurrency == null) {
                TangemLogger.e(
                    """
                        Could not get crypto currency for
                        |- $NETWORK_ID_KEY: $networkId
                        |- $TOKEN_ID_KEY: $tokenId
                    """.trimIndent(),
                )
                return@launch
            }

            analyticsEventHandler.send(PushNotificationAnalyticEvents.NotificationOpened(type.name))

            if (userWallet.isMultiCurrency) {
                appRouter.push(
                    route = AppRoute.CurrencyDetails(
                        userWalletId = userWallet.walletId,
                        currency = cryptoCurrency,
                    ),
                    onComplete = { walletDeepLinkActionTrigger.selectWallet(userWallet.walletId) },
                )
            } else {
                walletDeepLinkActionTrigger.selectWallet(userWallet.walletId)
            }

            if (transactionId != null) {
                when (type) {
                    NotificationType.SwapStatus,
                    NotificationType.OnrampStatus,
                    -> tokenDetailsDeepLinkActionTrigger.trigger(transactionId)
                    NotificationType.Promo,
                    NotificationType.IncomeTransactions,
                    NotificationType.Unknown,
                    -> Unit
                }
            }
            if (isFromOnNewIntent) fetchCurrency(userWallet, cryptoCurrency)
        }
    }

    private suspend fun fetchCurrency(userWallet: UserWallet, cryptoCurrency: CryptoCurrency) {
        val isMultiCurrency = userWallet.isMultiCurrency
        // single-currency wallet with token (NODL)
        userWallet is UserWallet.Cold &&
            userWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken()
        when {
            isMultiCurrency -> cryptoCurrencyBalanceFetcher(
                userWalletId = userWallet.walletId,
                currency = cryptoCurrency,
            )
            !isMultiCurrency -> walletBalanceFetcher(
                params = WalletBalanceFetcher.Params(
                    userWalletId = userWallet.walletId,
                    isPaymentAccountRefactorEnabled = tangemPayFeatureToggles.isTangemPayAccountsRefactorEnabled,
                ),
            )
        }
    }

    private suspend fun findCryptoCurrency(userWallet: UserWallet, networkId: String?, tokenId: String?) =
        if (userWallet.isMultiCurrency) {
            val derivationPath = queryParams[DERIVATION_PATH_KEY]

            getCryptoCurrencies(userWalletId = userWallet.walletId)?.firstOrNull { currency ->
                val isNetwork = currency.network.backendId.equals(networkId, ignoreCase = true)
                val isCurrency = currency.id.rawCurrencyId?.value?.equals(tokenId, ignoreCase = true) == true

                val isDefaultDerivation = currency.network.derivationPath is Network.DerivationPath.Card
                val isCustomDerivation = derivationPath?.equals(currency.network.derivationPath.value) == true
                val isCorrectDerivation = isDefaultDerivation || isCustomDerivation
                isNetwork && isCurrency && isCorrectDerivation
            }
        } else {
            singleAccountListSupplier.getSyncOrNull(userWalletId = userWallet.walletId)
                ?.mainAccount?.cryptoCurrencies?.first()
        }

    private suspend fun getCryptoCurrencies(userWalletId: UserWalletId): List<CryptoCurrency>? {
        return singleAccountListSupplier.getSyncOrNull(userWalletId)?.flattenCurrencies()
    }

    @AssistedFactory
    interface Factory : TokenDetailsDeepLinkHandler.Factory {
        override fun create(
            coroutineScope: CoroutineScope,
            queryParams: Map<String, String>,
            isFromOnNewIntent: Boolean,
        ): DefaultTokenDetailsDeepLinkHandler
    }
}