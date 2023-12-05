package com.tangem.feature.swap.router

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.core.navigation.ReduxNavController
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.tokendetails.navigation.TokenDetailsRouter
import java.lang.ref.WeakReference

internal class SwapRouter(
    private val fragmentManager: WeakReference<FragmentManager>,
    private val customTabsManager: CustomTabsManager,
    private val reduxNavController: ReduxNavController,
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
            fragmentManager.get()?.popBackStack()
        }
    }

    fun openUrl(url: String) {
        customTabsManager.openUrl(url)
    }

    fun openTokenDetails(userWalletId: UserWalletId, currency: CryptoCurrency) {
        reduxNavController.navigate(
            action = NavigationAction.NavigateTo(
                screen = AppScreen.WalletDetails,
                bundle = bundleOf(
                    TokenDetailsRouter.USER_WALLET_ID_KEY to userWalletId.stringValue,
                    TokenDetailsRouter.CRYPTO_CURRENCY_KEY to currency,
                ),
            ),
        )
    }
}

enum class SwapNavScreen {
    Main, Success, SelectToken
}
