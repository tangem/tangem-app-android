package com.tangem.feature.swap.router

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId

internal class SwapRouter(
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
            val selectTokensIndex = router.stack.getSelectTokensRouteIndexOrNull()

            /*
             * If select token screen is not in stack, then just pop to previous screen.
             * Otherwise, pop to previous screen that was before select token screen.
             */
            if (currentScreen == SwapNavScreen.Success && selectTokensIndex != null) {
                // find previous screen that was before select token
                val prevRoute = router.stack.getOrNull(index = selectTokensIndex - 1)

                if (prevRoute != null) {
                    router.popTo(prevRoute)
                } else {
                    router.pop()
                }
            } else {
                router.pop()
            }
        }
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

    private fun List<AppRoute>.getSelectTokensRouteIndexOrNull(): Int? {
        return this
            .indexOfFirst { it::class == AppRoute.SwapCrypto::class }
            .takeIf { it != -1 }
    }
}

enum class SwapNavScreen {
    Main, Success, SelectToken
}