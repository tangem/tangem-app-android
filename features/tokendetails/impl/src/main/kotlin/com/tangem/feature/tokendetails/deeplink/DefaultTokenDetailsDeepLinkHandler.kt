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
import com.tangem.domain.account.fetcher.SingleAccountListFetcher
import com.tangem.domain.account.status.utils.CryptoCurrencyBalanceFetcher
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.notifications.models.NotificationType
import com.tangem.domain.tokens.wallet.WalletBalanceFetcher
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.SelectWalletUseCase
import com.tangem.features.pushnotifications.api.analytics.PushNotificationAnalyticEvents
import com.tangem.features.tokendetails.deeplink.TokenDetailsDeepLinkHandler
import com.tangem.features.wallet.deeplink.WalletDeepLinkActionTrigger
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

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
    private val singleAccountListSupplier: SingleAccountListSupplier,
    private val singleAccountListFetcher: SingleAccountListFetcher,
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

            // Refresh the portfolio before searching so a token just added on the backend is present locally.
            refreshAccountsIfNeeded(userWallet)

            val cryptoCurrency = findCryptoCurrency(userWallet = userWallet, networkId = networkId, tokenId = tokenId)

            if (cryptoCurrency == null) {
                TangemLogger.e(
                    """
                        Could not get crypto currency for
                        |- $NETWORK_ID_KEY: $networkId
                        |- $TOKEN_ID_KEY: $tokenId
                    """.trimIndent(),
                )
                // Token is not in the response (not indexed yet / backend error): go to main, do not add.
                appRouter.popTo(AppRoute.Wallet)
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

    /**
     * Refreshes wallet accounts so a token just added on the backend appears in the local portfolio.
     *
     * Only when the app was open on push tap ([isFromOnNewIntent]) and the wallet is multi-currency:
     * on cold start the fresh list is already loaded by the regular auth flow, and single-currency
     * wallets have a fixed token. The fetch is best-effort — on failure we fall through and try the
     * current cache, so existing tokens (e.g. swap/onramp pushes) still open without regression.
     */
    private suspend fun refreshAccountsIfNeeded(userWallet: UserWallet) {
        if (isFromOnNewIntent && userWallet.isMultiCurrency) {
            singleAccountListFetcher(SingleAccountListFetcher.Params(userWalletId = userWallet.walletId))
                .onLeft { TangemLogger.e("Error on refreshing wallet accounts", it) }
        }
    }

    private suspend fun fetchCurrency(userWallet: UserWallet, cryptoCurrency: CryptoCurrency) {
        val isMultiCurrency = userWallet.isMultiCurrency
        when {
            isMultiCurrency -> cryptoCurrencyBalanceFetcher(
                userWalletId = userWallet.walletId,
                currency = cryptoCurrency,
            )
            !isMultiCurrency -> walletBalanceFetcher(
                params = WalletBalanceFetcher.Params(userWalletId = userWallet.walletId),
            )
        }
    }

    private suspend fun findCryptoCurrency(userWallet: UserWallet, networkId: String?, tokenId: String?) =
        if (userWallet.isMultiCurrency) {
            val derivationPath = queryParams[DERIVATION_PATH_KEY]
            val matches = { currency: CryptoCurrency -> currency.matches(networkId, tokenId, derivationPath) }

            singleAccountListSupplier.getSyncOrNull(userWallet.walletId)?.flattenCurrencies()?.firstOrNull(matches)
                // getSyncOrNull returns the stale SharedFlow replay just after a fetch; wait for the refreshed list.
                ?: awaitCryptoCurrency(userWallet.walletId, matches)
        } else {
            singleAccountListSupplier.getSyncOrNull(userWalletId = userWallet.walletId)
                ?.mainAccount?.cryptoCurrencies?.first()
        }

    private suspend fun awaitCryptoCurrency(
        userWalletId: UserWalletId,
        matches: (CryptoCurrency) -> Boolean,
    ): CryptoCurrency? = withTimeoutOrNull(TOKEN_APPEARANCE_TIMEOUT_MILLIS) {
        singleAccountListSupplier(userWalletId)
            .mapNotNull { accountList -> accountList.flattenCurrencies().firstOrNull(matches) }
            .firstOrNull()
    }

    private fun CryptoCurrency.matches(networkId: String?, tokenId: String?, derivationPath: String?): Boolean {
        val isNetwork = network.rawId.equals(networkId, ignoreCase = true)
        val isCurrency = id.rawCurrencyId?.value?.equals(tokenId, ignoreCase = true) == true
        val isDefaultDerivation = network.derivationPath is Network.DerivationPath.Card
        val isCustomDerivation = derivationPath?.equals(network.derivationPath.value) == true
        return isNetwork && isCurrency && (isDefaultDerivation || isCustomDerivation)
    }

    @AssistedFactory
    interface Factory : TokenDetailsDeepLinkHandler.Factory {
        override fun create(
            coroutineScope: CoroutineScope,
            queryParams: Map<String, String>,
            isFromOnNewIntent: Boolean,
        ): DefaultTokenDetailsDeepLinkHandler
    }

    private companion object {
        const val TOKEN_APPEARANCE_TIMEOUT_MILLIS = 3_000L
    }
}