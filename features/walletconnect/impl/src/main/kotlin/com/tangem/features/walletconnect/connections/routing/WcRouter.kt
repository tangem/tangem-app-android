package com.tangem.features.walletconnect.connections.routing

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.navigate
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.decompose.navigation.Router
import kotlin.reflect.KClass

/**
 * Router of the WalletConnect slot: [WcInnerRoute]s open and close the slot, any other route goes to [fallBackRouter].
 */
internal class WcRouter(
    val slotNavigation: SlotNavigation<WcInnerRoute>,
    private val fallBackRouter: Router,
) : Router {

    override fun push(route: Route, onComplete: (Boolean) -> Unit) {
        if (route is WcInnerRoute) {
            slotNavigation.activate(route)
            onComplete(true)
        } else {
            fallBackRouter.push(route, onComplete)
        }
    }

    override fun replaceCurrent(route: Route, onComplete: (Boolean) -> Unit) {
        if (route !is WcInnerRoute) fallBackRouter.replaceCurrent(route, onComplete)
    }

    override fun replaceAll(vararg routes: Route, onComplete: (Boolean) -> Unit) {
        if (routes.none { it is WcInnerRoute }) fallBackRouter.replaceAll(*routes, onComplete = onComplete)
    }

    override fun pop(onComplete: (Boolean) -> Unit) {
        slotNavigation.navigate { null }
    }

    override fun popTo(route: Route, onComplete: (Boolean) -> Unit) {
        if (route !is WcInnerRoute) fallBackRouter.popTo(route, onComplete)
    }

    override fun popTo(routeClass: KClass<out Route>, onComplete: (Boolean) -> Unit) {
        if (!isWcRoute(routeClass)) fallBackRouter.popTo(routeClass, onComplete)
    }

    override fun <R : Route> popToMatching(
        routeClass: KClass<R>,
        predicate: (R) -> Boolean,
        onComplete: (Boolean) -> Unit,
    ) {
        if (!isWcRoute(routeClass)) fallBackRouter.popToMatching(routeClass, predicate, onComplete)
    }

    /** The slot holds a single route, so the transformer always applies to the stack of [fallBackRouter]. */
    override fun navigate(transformer: (stack: List<Route>) -> List<Route>, onComplete: (Boolean) -> Unit) {
        fallBackRouter.navigate(transformer, onComplete)
    }

    override suspend fun contains(route: Route): Boolean {
        return route !is WcInnerRoute && fallBackRouter.contains(route)
    }

    private fun isWcRoute(routeClass: KClass<out Route>): Boolean {
        return WcInnerRoute::class.java.isAssignableFrom(routeClass.java)
    }
}