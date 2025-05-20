package com.tangem.feature.tokendetails.deeplink

import arrow.core.getOrElse
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.domain.notifications.models.NotificationType
import com.tangem.domain.tokens.GetCryptoCurrenciesUseCase
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.features.tokendetails.deeplink.TokenDetailsDeepLinkHandler
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

internal class DefaultTokenDetailsDeepLinkHandler @AssistedInject constructor(
    @Assisted private val params: Map<String, String>,
    @Assisted private val scope: CoroutineScope,
    private val appRouter: AppRouter,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val getCryptoCurrenciesUseCase: GetCryptoCurrenciesUseCase,
) : TokenDetailsDeepLinkHandler {

    init {
        handleDeepLink()
    }

    private fun handleDeepLink() {
        // It is okay here, we are navigating from outside, and there is no other way to getting UserWallet
        val userWalletId = params[WALLET_ID_KEY]?.let(::UserWalletId)
            ?: getSelectedWalletSyncUseCase().getOrNull()?.walletId

        if (userWalletId == null) {
            Timber.e("Error on getting user wallet")
            return
        }

        val networkId = params[NETWORK_ID_KEY]
        val tokenId = params[TOKEN_ID_KEY]
        val type = NotificationType.getType(params[TYPE_KEY])

        if (type == NotificationType.Promo) {
            scope.launch {
                val cryptoCurrency = getCryptoCurrenciesUseCase(userWalletId = userWalletId).getOrElse {
                    Timber.e("Error on getting crypto currency list")
                    return@launch
                }.firstOrNull {
                    it.id.rawNetworkId == networkId && it.id.rawCurrencyId?.value == tokenId
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

                appRouter.push(
                    AppRoute.CurrencyDetails(
                        userWalletId = userWalletId,
                        currency = cryptoCurrency,
                    ),
                )
            }
        }
    }

    @AssistedFactory
    interface Factory : TokenDetailsDeepLinkHandler.Factory {
        override fun create(
            coroutineScope: CoroutineScope,
            params: Map<String, String>,
        ): DefaultTokenDetailsDeepLinkHandler
    }

    private companion object {
        const val WALLET_ID_KEY = "walletId"
        const val NETWORK_ID_KEY = "network_id"
        const val TYPE_KEY = "type"
        const val TOKEN_ID_KEY = "token_id"
    }
}