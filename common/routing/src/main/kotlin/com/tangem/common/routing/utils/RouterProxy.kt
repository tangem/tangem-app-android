package com.tangem.common.routing.utils

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.decompose.navigation.Router
import kotlin.reflect.KClass

/**
 * Temporary solution to convert [AppRouter] to [Router].

 * (through manual ComponentContext creation).
 *
 * **Will be removed when all screens will be migrated to Decompose.**
 *
 * @return [Router] that wraps [AppRouter].
 */
fun AppRouter.asRouter(): Router {
    return RouterProxy(appRouter = this)
}

private class RouterProxy(
    private val appRouter: AppRouter,
) : Router {
    override fun push(route: Route, onComplete: (isSuccess: Boolean) -> Unit) {
        if (route is AppRoute) {
            appRouter.push(route, onComplete)
        }
    }

    override fun replaceAll(vararg routes: Route, onComplete: (isSuccess: Boolean) -> Unit) {
        routes.filterIsInstance<AppRoute>().let {
            appRouter.replaceAll(*it.toTypedArray(), onComplete = onComplete)
        }
    }

    override fun pop(onComplete: (isSuccess: Boolean) -> Unit) {
        appRouter.pop(onComplete)
    }

    override fun popTo(route: Route, onComplete: (isSuccess: Boolean) -> Unit) {
        if (route is AppRoute) {
            appRouter.popTo(route, onComplete)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun popTo(routeClass: KClass<out Route>, onComplete: (isSuccess: Boolean) -> Unit) {
        appRouter.popTo(routeClass as KClass<out AppRoute>, onComplete)
    }
}