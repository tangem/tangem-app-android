package com.tangem.feature.swap.router

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId

internal class SwapRouter(
    private val customTabsManager: CustomTabsManager,
    private val router: AppRouter,
) {

    var currentScreen by mutableStateOf(SwapNavScreen.Main)
        private set

    fun openScreen(screen: SwapNavScreen) {
        currentScreen = screen
    }

    fun back() {
        if (currentScreen == SwapNavScreen.SelectToken) {
            currentScreen = SwapNavScreen.Main
        } else {
            router.pop()
        }
    }

    fun openUrl(url: String) {
        customTabsManager.openUrl(url)
    }

    fun openTokenDetails(userWalletId: UserWalletId, currency: CryptoCurrency) {
        val route = AppRoute.CurrencyDetails(
            userWalletId = userWalletId,
            currency = currency,
        )

        if (route in router.stack) {
            router.popTo(route)
        } else {
            router.pop {
                router.push(route)
            }
        }
    }
}

enum class SwapNavScreen {
    Main, Success, SelectToken
}
