package com.tangem.features.onramp.deeplink

import arrow.core.getOrElse
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.tokens.GetCryptoCurrencyUseCase
import com.tangem.domain.tokens.model.analytics.TokenScreenAnalyticsEvent
import com.tangem.domain.wallets.models.isMultiCurrency
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.features.onramp.OnrampFeatureToggles
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

internal class DefaultBuyDeepLinkHandler @AssistedInject constructor(
    @Assisted scope: CoroutineScope,
    onrampFeatureToggles: OnrampFeatureToggles,
    getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    getCryptoCurrencyUseCase: GetCryptoCurrencyUseCase,
    analyticsEventHandler: AnalyticsEventHandler,
) : BuyDeepLinkHandler {

    init {
        // It is okay here, we are navigating from outside, and there is no other way to getting UserWallet
        getSelectedWalletSyncUseCase().fold(
            ifLeft = {
                Timber.e("Error on getting user wallet: $it")
            },
            ifRight = { userWallet ->
                if (!onrampFeatureToggles.isFeatureEnabled && !userWallet.isMultiCurrency) {
                    scope.launch {
                        val cryptoCurrency = getCryptoCurrencyUseCase(userWallet.walletId).getOrElse {
                            Timber.e("Error on getting cryptoCurrency: $it")
                            return@launch
                        }
                        analyticsEventHandler.send(TokenScreenAnalyticsEvent.Bought(cryptoCurrency.symbol))
                    }
                }
            },
        )
    }

    @AssistedFactory
    interface Factory : BuyDeepLinkHandler.Factory {
        override fun create(coroutineScope: CoroutineScope): DefaultBuyDeepLinkHandler
    }
}