package com.tangem.tap.common.redux

import com.tangem.tap.common.redux.global.GlobalState
import com.tangem.tap.common.redux.global.globalMiddleware
import com.tangem.tap.common.redux.navigation.NavigationState
import com.tangem.tap.common.redux.navigation.navigationMiddleware
import com.tangem.tap.features.details.redux.DetailsMiddleware
import com.tangem.tap.features.details.redux.DetailsState
import com.tangem.tap.features.disclaimer.redux.DisclaimerMiddleware
import com.tangem.tap.features.disclaimer.redux.DisclaimerState
import com.tangem.tap.features.home.redux.HomeMiddleware
import com.tangem.tap.features.home.redux.HomeState
import com.tangem.tap.features.send.redux.middlewares.sendMiddleware
import com.tangem.tap.features.send.redux.states.SendState
import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.features.wallet.redux.walletMiddleware
import org.rekotlin.Middleware
import org.rekotlin.StateType

data class AppState(
        val navigationState: NavigationState = NavigationState(),
        val globalState: GlobalState = GlobalState(),
        val homeState: HomeState = HomeState(),
        val walletState: WalletState = WalletState(),
        val sendState: SendState = SendState(),
        val detailsState: DetailsState = DetailsState(),
        val disclaimerState: DisclaimerState = DisclaimerState()
) : StateType {

    companion object {
        fun getMiddleware(): List<Middleware<AppState>> {
            return listOf(
                    logMiddleware, navigationMiddleware, notificationsMiddleware, globalMiddleware,
                    HomeMiddleware().homeMiddleware, walletMiddleware, sendMiddleware,
                    DetailsMiddleware().detailsMiddleware,
                    DisclaimerMiddleware().disclaimerMiddleware
            )
        }
    }
}

