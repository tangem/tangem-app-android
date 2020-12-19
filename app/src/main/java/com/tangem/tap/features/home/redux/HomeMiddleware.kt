package com.tangem.tap.features.home.redux

import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat.startActivity
import com.tangem.common.CompletionResult
import com.tangem.tap.common.analytics.FirebaseAnalyticsHandler
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.preferencesStorage
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.rekotlin.Middleware

class HomeMiddleware {
    val homeMiddleware: Middleware<AppState> = { dispatch, state ->
        { next ->
            { action ->
                when (action) {
                    is HomeAction.CheckIfFirstLaunch -> {
                        store.dispatch(
                                HomeAction.CheckIfFirstLaunch.Result(preferencesStorage.getCountOfLaunches() == 1)
                        )
                    }
                    is HomeAction.ReadCard -> {
                        scope.launch {
                            val result = tangemSdkManager.scanNote(FirebaseAnalyticsHandler)
                            withContext(Dispatchers.Main) {
                                when (result) {
                                    is CompletionResult.Success -> {
                                        tangemSdkManager.changeDisplayedCardIdNumbersCount(result.data.card)
                                        store.dispatch(GlobalAction.RestoreAppCurrency)
                                        store.state.globalState.tapWalletManager.onCardScanned(result.data)
                                        showDisclaimerOrNavigateToWallet()
                                    }
                                }
                            }
                        }
                    }
                    is HomeAction.GoToShop -> {
                        val uri = Uri.parse(CARD_SHOP_URI)
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        startActivity(action.context, intent, null)
                    }
                }
                next(action)
            }
        }
    }

    private fun showDisclaimerOrNavigateToWallet() {
        if (!preferencesStorage.wasDisclaimerAccepted()) {
            store.dispatch(NavigationAction.NavigateTo(AppScreen.Disclaimer))
            return
        }
        if (store.state.walletState.twinCardsState != null) {
            val showOnboarding = !preferencesStorage.wasTwinsOnboardingShown()
            if (showOnboarding) {
                store.dispatch(NavigationAction.NavigateTo(AppScreen.TwinsOnboarding))
                return
            }
        }
        store.dispatch(NavigationAction.NavigateTo(AppScreen.Wallet))
    }

    companion object {
        private const val CARD_SHOP_URI =
                "https://shop.tangem.com/?afmc=1i&utm_campaign=1i&utm_source=leaddyno&utm_medium=affiliate"
    }
}




