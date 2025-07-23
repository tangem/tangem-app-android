package com.tangem.tap.features.wallet.redux.middlewares

import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.Analytics
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.tokens.legacy.TradeCryptoAction
import com.tangem.tap.common.analytics.events.Token
import com.tangem.tap.common.apptheme.MutableAppThemeModeHolder
import com.tangem.tap.common.extensions.dispatchNavigationAction
import com.tangem.tap.common.extensions.dispatchOpenUrl
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.demo.DemoHelper
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.store
import org.rekotlin.Middleware

@Deprecated("Will be removed soon")
object TradeCryptoMiddleware {

    val middleware: Middleware<AppState> = { _, appState ->
        { nextDispatch ->
            { action ->
                if (action is TradeCryptoAction) {
                    handle(appState, action)
                }
                nextDispatch(action)
            }
        }
    }

    private fun handle(state: () -> AppState?, action: TradeCryptoAction) {
        if (DemoHelper.tryHandle(state, action)) return

        when (action) {
            is TradeCryptoAction.FinishSelling -> openReceiptUrl(action.transactionId)
            is TradeCryptoAction.Sell -> proceedSellAction(action)
        }
    }

    private fun proceedSellAction(action: TradeCryptoAction.Sell) {
        val networkAddress = action.cryptoCurrencyStatus.value.networkAddress
            ?.defaultAddress
            ?.let(NetworkAddress.Address::value)
            ?: return
        val currency = action.cryptoCurrencyStatus.currency

        store.inject(DaggerGraphState::appStateHolder).sellService?.getUrl(
            cryptoCurrency = currency,
            fiatCurrencyName = action.appCurrencyCode,
            walletAddress = networkAddress,
            isDarkTheme = MutableAppThemeModeHolder.isDarkThemeActive,
        )?.let {
            store.dispatchOpenUrl(it)
            Analytics.send(Token.Withdraw.ScreenOpened())
        }
    }

    private fun openReceiptUrl(transactionId: String) {
        store.dispatchNavigationAction(AppRouter::pop)

        val sellService = store.inject(DaggerGraphState::appStateHolder).sellService
        sellService?.getSellCryptoReceiptUrl(transactionId = transactionId)
            ?.let(store::dispatchOpenUrl)
    }
}