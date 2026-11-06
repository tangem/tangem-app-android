package com.tangem.features.earn.impl

import com.tangem.core.decompose.navigation.Route
import com.tangem.core.decompose.navigation.Router
import com.tangem.features.earn.product.api.navigation.EarnNavigator

/** The navigator of an Earn screen: every route of an action handler goes onto the router of the screen's stack. */
internal class InnerEarnNavigator(private val router: Router) : EarnNavigator {

    override fun navigate(route: Route) {
        router.push(route)
    }
}