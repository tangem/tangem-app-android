package com.tangem.tap.features.home.redux

import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat.startActivity
import com.tangem.common.CompletionResult
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.rekotlin.Middleware

val homeMiddleware: Middleware<AppState> = { dispatch, state ->
    { next ->
        { action ->
            when (action) {
                is HomeAction.ReadCard -> {
                    scope.launch {
                        val result = tangemSdkManager.scanNote()
                        withContext(Dispatchers.Main) {
                            when (result) {
                                is CompletionResult.Success -> {
                                    store.dispatch(GlobalAction.LoadCard(result.data.card))
                                    store.dispatch(GlobalAction.LoadWalletManager(result.data.walletManager))
                                    store.dispatch(WalletAction.LoadWallet)
                                    store.dispatch(WalletAction.LoadFiatRate)
                                    store.dispatch(WalletAction.LoadPayId)
                                    store.dispatch(NavigationAction.NavigateTo(AppScreen.Wallet))
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

private const val CARD_SHOP_URI = "https://shop.tangem.com/?afmc=1i&utm_campaign=1i&utm_source=leaddyno&utm_medium=affiliate"

