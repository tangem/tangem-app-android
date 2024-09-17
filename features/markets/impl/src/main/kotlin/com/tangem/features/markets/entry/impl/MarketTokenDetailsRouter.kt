package com.tangem.features.markets.entry.impl

import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.popWhile
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.decompose.navigation.Router
import kotlin.reflect.KClass

internal class MarketTokenDetailsRouter(
    private val contextRouter: Router,
    private val stackNavigation: StackNavigation<MarketsEntryChildFactory.Child>,
) : Router by contextRouter {

    override fun pop(onComplete: (isSuccess: Boolean) -> Unit) {
        stackNavigation.popWhile({ it != MarketsEntryChildFactory.Child.TokenList }, onComplete)
    }

    override fun popTo(route: Route, onComplete: (isSuccess: Boolean) -> Unit) {
        /** Not allowed */
    }

    override fun popTo(routeClass: KClass<out Route>, onComplete: (isSuccess: Boolean) -> Unit) {
        /** Not allowed */
    }
}