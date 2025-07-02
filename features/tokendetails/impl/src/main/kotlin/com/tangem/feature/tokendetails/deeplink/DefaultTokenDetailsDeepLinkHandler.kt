package com.tangem.feature.tokendetails.deeplink

import arrow.core.getOrElse
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.deeplink.DeeplinkConst.NETWORK_ID_KEY
import com.tangem.core.deeplink.DeeplinkConst.TOKEN_ID_KEY
import com.tangem.core.deeplink.DeeplinkConst.TYPE_KEY
import com.tangem.core.deeplink.DeeplinkConst.WALLET_ID_KEY
import com.tangem.domain.notifications.models.NotificationType
import com.tangem.domain.tokens.FetchCurrencyStatusUseCase
import com.tangem.domain.tokens.GetCryptoCurrenciesUseCase
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.features.pushnotifications.api.analytics.PushNotificationAnalyticEvents
import com.tangem.features.tokendetails.deeplink.TokenDetailsDeepLinkHandler
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
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val getCryptoCurrenciesUseCase: GetCryptoCurrenciesUseCase,
    private val fetchCurrencyStatusUseCase: FetchCurrencyStatusUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : TokenDetailsDeepLinkHandler {

    init {
        handleDeepLink()
    }

    private fun handleDeepLink() {
        val networkId = queryParams[NETWORK_ID_KEY]
        val tokenId = queryParams[TOKEN_ID_KEY]
        val type = NotificationType.getType(queryParams[TYPE_KEY])

        // It is okay here, we are navigating from outside, and there is no other way to getting UserWallet
        val selectedUserWalletId = getSelectedWalletSyncUseCase().getOrNull()?.walletId
        val walletId = queryParams[WALLET_ID_KEY]?.let(::UserWalletId) ?: selectedUserWalletId

        // If selected user wallet is different than from deeplink - ignore deeplink
        // If selected user wallet is null - ignore deeplink
        if (walletId != selectedUserWalletId || selectedUserWalletId == null) {
            Timber.e("Error on getting user wallet")
            return
        }

        scope.launch {
            val cryptoCurrency = getCryptoCurrenciesUseCase(userWalletId = selectedUserWalletId).getOrElse {
                Timber.e("Error on getting crypto currency list")
                return@launch
            }.firstOrNull {
                val isNetwork = it.network.backendId.equals(networkId, ignoreCase = true)
                val isCurrency = it.id.rawCurrencyId?.value?.equals(tokenId, ignoreCase = true) == true
                isNetwork && isCurrency
            }

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

            appRouter.push(
                AppRoute.CurrencyDetails(
                    userWalletId = selectedUserWalletId,
                    currency = cryptoCurrency,
                ),
            )

            if (isFromOnNewIntent) {
                fetchCurrencyStatusUseCase.invoke(
                    userWalletId = selectedUserWalletId,
                    id = cryptoCurrency.id,
                    refresh = true,
                )
            }
        }
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