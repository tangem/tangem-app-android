package com.tangem.features.earn.impl.fake

import com.tangem.core.decompose.navigation.Route
import com.tangem.features.earn.product.api.navigation.EarnNavigator

class FakeEarnNavigator : EarnNavigator {

    private val recorded = mutableListOf<Route>()

    val routes: List<Route>
        get() = recorded.toList()

    override fun navigate(route: Route) {
        recorded += route
    }
}

/** A route that is not an Earn route: what a product without the shared flow hands to the navigator. */
data object FakeForeignRoute : Route