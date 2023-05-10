package com.tangem.tap.features.customtoken.impl.presentation.routers

import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.store

/** Default implementation of custom token feature router */
internal class DefaultCustomTokenRouter : CustomTokenRouter {

    override fun popBackStack() {
        store.dispatch(NavigationAction.PopBackTo())
    }
}
