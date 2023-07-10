package com.tangem.tap.features.customtoken.impl.presentation.routers

import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.tap.store

/** Default implementation of custom token feature router */
internal class DefaultCustomTokenRouter : CustomTokenRouter {

    override fun popBackStack() {
        store.dispatch(NavigationAction.PopBackTo())
    }

    override fun openWalletScreen() {
        store.dispatch(NavigationAction.PopBackTo(screen = AppScreen.Wallet))
    }
}