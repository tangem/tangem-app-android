package com.tangem.tap.common.redux

import com.tangem.tap.common.redux.global.GlobalMiddleware
import com.tangem.tap.common.redux.global.GlobalState
import com.tangem.tap.common.redux.legacy.LegacyMiddleware
import com.tangem.tap.features.details.redux.DetailsMiddleware
import com.tangem.tap.features.details.redux.DetailsState
import com.tangem.tap.features.onboarding.products.wallet.redux.BackupMiddleware
import com.tangem.tap.features.wallet.redux.middlewares.TradeCryptoMiddleware
import com.tangem.tap.features.welcome.redux.WelcomeMiddleware
import com.tangem.tap.features.welcome.redux.WelcomeState
import com.tangem.tap.proxy.redux.DaggerGraphMiddleware
import com.tangem.tap.proxy.redux.DaggerGraphState
import org.rekotlin.Middleware
import org.rekotlin.StateType

data class AppState(
    val globalState: GlobalState = GlobalState(),
    val detailsState: DetailsState = DetailsState(),
    val welcomeState: WelcomeState = WelcomeState(),
    val daggerGraphState: DaggerGraphState = DaggerGraphState(),
) : StateType {

    companion object {
        fun getMiddleware(): List<Middleware<AppState>> {
            return listOf(
                logMiddleware,
                GlobalMiddleware.handler,
                DetailsMiddleware().detailsMiddleware,
                BackupMiddleware().backupMiddleware,
                WelcomeMiddleware().middleware,
                LockUserWalletsTimerMiddleware().middleware,
                AccessCodeRequestPolicyMiddleware().middleware,
                DaggerGraphMiddleware.daggerGraphMiddleware,
                LegacyMiddleware.legacyMiddleware,
                TradeCryptoMiddleware.middleware,
            )
        }
    }
}