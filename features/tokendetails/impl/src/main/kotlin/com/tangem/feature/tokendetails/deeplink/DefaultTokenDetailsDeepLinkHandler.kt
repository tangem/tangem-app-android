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
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.notifications.models.NotificationType
import com.tangem.domain.tokens.*
import com.tangem.domain.tokens.wallet.WalletBalanceFetcher
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.SelectWalletUseCase
import com.tangem.features.pushnotifications.api.analytics.PushNotificationAnalyticEvents
import com.tangem.features.tokendetails.deeplink.TokenDetailsDeepLinkHandler
import com.tangem.features.wallet.deeplink.WalletDeepLinkActionTrigger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

@Suppress("LongParameterList")
internal class DefaultTokenDetailsDeepLinkHandler @AssistedInject constructor(
    @Assisted private val scope: CoroutineScope,
    @Assisted private val queryParams: Map<String, String>,
    @Assisted private val isFromOnNewIntent: Boolean,
    private val appRouter: AppRouter,
    private val selectWalletUseCase: SelectWalletUseCase,
    private val getCryptoCurrenciesUseCase: GetCryptoCurrenciesUseCase,
    private val getCryptoCurrencyUseCase: GetCryptoCurrencyUseCase,
    private val fetchCurrencyStatusUseCase: FetchCurrencyStatusUseCase,
    private val tokenDetailsDeepLinkActionTrigger: TokenDetailsDeepLinkActionTrigger,
    private val walletDeepLinkActionTrigger: WalletDeepLinkActionTrigger,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val tokensFeatureToggles: TokensFeatureToggles,
    private val walletBalanceFetcher: WalletBalanceFetcher,
    private val fetchCardTokenListUseCase: FetchCardTokenListUseCase,
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
            val userWallet = userWalletId?.let { getUserWalletUseCase(userWalletId) }?.getOrNull()
            // If wallet to select is null or locked, ignore deeplink
            if (userWallet == null || userWallet.isLocked) {
                Timber.e("Error on getting user wallet")
                return@launch
            }
            if (selectWalletUseCase(userWalletId).getOrNull() == null) {
                Timber.e("Error on selecting user wallet")
                return@launch
            }

            val cryptoCurrency = getCryptoCurrency(userWallet = userWallet, networkId = networkId, tokenId = tokenId)

            if (cryptoCurrency == null) {
                Timber.e(
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
        val isSingleWalletWithToken = userWallet is UserWallet.Cold &&
            userWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken()
        when {
            isMultiCurrency -> fetchCurrencyStatusUseCase.invoke(
                userWalletId = userWallet.walletId,
                id = cryptoCurrency.id,
            )
            !isMultiCurrency && tokensFeatureToggles.isWalletBalanceFetcherEnabled ->
                walletBalanceFetcher(params = WalletBalanceFetcher.Params(userWalletId = userWallet.walletId))
            // remove below after delete tokensFeatureToggles.isWalletBalanceFetcherEnabled
            !isMultiCurrency && userWallet is UserWallet.Cold && isSingleWalletWithToken ->
                fetchCardTokenListUseCase.invoke(
                    userWalletId = userWallet.walletId,
                    refresh = true,
                )
            !isMultiCurrency -> fetchCurrencyStatusUseCase.invoke(
                userWalletId = userWallet.walletId,
                refresh = true,
            )
        }
    }

    private suspend fun getCryptoCurrency(userWallet: UserWallet, networkId: String?, tokenId: String?) =
        if (userWallet.isMultiCurrency) {
            val derivationPath = queryParams[DERIVATION_PATH_KEY]

            getCryptoCurrenciesUseCase(userWalletId = userWallet.walletId)
                .getOrNull()
                ?.firstOrNull {
                    val isNetwork = it.network.backendId.equals(networkId, ignoreCase = true)
                    val isCurrency = it.id.rawCurrencyId?.value?.equals(tokenId, ignoreCase = true) == true

                    val isDefaultDerivation = it.network.derivationPath is Network.DerivationPath.Card
                    val isCustomDerivation = derivationPath?.equals(it.network.derivationPath.value) == true
                    val isCorrectDerivation = isDefaultDerivation || isCustomDerivation
                    isNetwork && isCurrency && isCorrectDerivation
                }
        } else {
            getCryptoCurrencyUseCase(userWalletId = userWallet.walletId).getOrNull()
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