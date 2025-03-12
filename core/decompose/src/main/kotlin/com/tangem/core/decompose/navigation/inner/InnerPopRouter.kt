package com.tangem.core.decompose.navigation.inner

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.decompose.navigation.Router
import kotlin.reflect.KClass

/**
 * Creates an router for overriding the pop operation.
 *
 * @param popCallback The callback to pop the route.
 */
@Suppress("FunctionName")
inline fun AppComponentContext.InnerPopRouter(
    crossinline popCallback: ((onComplete: (Boolean) -> Unit) -> Unit),
): Router = object : Router {
    override fun push(route: Route, onComplete: (Boolean) -> Unit) {
        router.push(route, onComplete)
    }

    override fun replaceAll(vararg routes: Route, onComplete: (Boolean) -> Unit) {
        router.replaceAll(*routes, onComplete = onComplete)
    }

    override fun pop(onComplete: (Boolean) -> Unit) {
        popCallback(onComplete)
    }

    override fun popTo(route: Route, onComplete: (Boolean) -> Unit) {
        router.popTo(route, onComplete)
    }

    override fun popTo(routeClass: KClass<out Route>, onComplete: (Boolean) -> Unit) {
        router.popTo(routeClass, onComplete)
    }
}