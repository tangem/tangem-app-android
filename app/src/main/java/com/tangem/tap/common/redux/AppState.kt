package com.tangem.tap.common.redux

import com.tangem.tap.common.redux.global.GlobalState
import com.tangem.tap.common.redux.navigation.NavigationState
import com.tangem.tap.common.redux.navigation.navigationMiddleware
import com.tangem.tap.features.home.redux.homeMiddleware
import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.features.wallet.redux.walletMiddleware
import org.rekotlin.Middleware
import org.rekotlin.StateType

data class AppState(
        val navigationState: NavigationState = NavigationState(),
        val globalState: GlobalState = GlobalState(),
        val walletState: WalletState = WalletState()
) : StateType {

    companion object {
        fun getMiddleware(): List<Middleware<AppState>> {
            return listOf(
                navigationMiddleware, notificationsMiddleware,
                homeMiddleware, walletMiddleware,
            )
        }
    }
}


