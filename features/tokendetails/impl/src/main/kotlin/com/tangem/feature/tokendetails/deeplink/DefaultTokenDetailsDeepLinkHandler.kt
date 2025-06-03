package com.tangem.feature.tokendetails.deeplink

import arrow.core.getOrElse
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.deeplink.DeeplinkConst.NETWORK_ID_KEY
import com.tangem.core.deeplink.DeeplinkConst.TOKEN_ID_KEY
import com.tangem.core.deeplink.DeeplinkConst.TYPE_KEY
import com.tangem.core.deeplink.DeeplinkConst.WALLET_ID_KEY
import com.tangem.domain.notifications.models.NotificationType
import com.tangem.domain.tokens.FetchCurrencyStatusUseCase
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

@Suppress("LongParameterList")
internal class DefaultTokenDetailsDeepLinkHandler @AssistedInject constructor(
    @Assisted private val scope: CoroutineScope,
    @Assisted private val queryParams: Map<String, String>,
    @Assisted private val isFromOnNewIntent: Boolean,
    private val appRouter: AppRouter,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val getCryptoCurrenciesUseCase: GetCryptoCurrenciesUseCase,
    private val fetchCurrencyStatusUseCase: FetchCurrencyStatusUseCase,
) : TokenDetailsDeepLinkHandler {

    init {
        handleDeepLink()
    }

    private fun handleDeepLink() {
        // It is okay here, we are navigating from outside, and there is no other way to getting UserWallet
        val userWalletId = queryParams[WALLET_ID_KEY]?.let(::UserWalletId)
            ?: getSelectedWalletSyncUseCase().getOrNull()?.walletId

        if (userWalletId == null) {
            Timber.e("Error on getting user wallet")
            return
        }

        val networkId = queryParams[NETWORK_ID_KEY]
        val tokenId = queryParams[TOKEN_ID_KEY]
        val type = NotificationType.getType(queryParams[TYPE_KEY])

        if (type != NotificationType.Unknown) {
            scope.launch {
                val cryptoCurrency = getCryptoCurrenciesUseCase(userWalletId = userWalletId).getOrElse {
                    Timber.e("Error on getting crypto currency list")
                    return@launch
                }.firstOrNull {
                    it.network.backendId == networkId &&
                        it.id.rawCurrencyId?.value == tokenId
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

                if (isFromOnNewIntent) {
                    fetchCurrencyStatusUseCase.invoke(
                        userWalletId = userWalletId,
                        id = cryptoCurrency.id,
                        refresh = true,
                    )
                }
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