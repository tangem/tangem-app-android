package com.tangem.features.walletconnect.connections.routing

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.navigate
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.decompose.navigation.Router
import com.tangem.features.walletconnect.connections.routes.WcInnerRoute
import kotlin.reflect.KClass

internal class WcRouter(
    private val stackNavigation: SlotNavigation<WcInnerRoute>,
) : Router {

    override fun push(route: Route, onComplete: (Boolean) -> Unit) {
        if (route !is WcInnerRoute) return
        stackNavigation.activate(route)
    }

    override fun replaceAll(vararg routes: Route, onComplete: (Boolean) -> Unit) {
        /** Not allowed */
    }

    override fun pop(onComplete: (Boolean) -> Unit) {
        stackNavigation.navigate { null }
    }

    override fun popTo(route: Route, onComplete: (Boolean) -> Unit) {
        /** Not allowed */
    }

    override fun popTo(routeClass: KClass<out Route>, onComplete: (Boolean) -> Unit) {
        /** Not allowed */
    }
}