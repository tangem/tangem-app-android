package com.tangem.core.decompose.navigation.inner

import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.router.stack.replaceCurrent
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.utils.stack
import kotlinx.coroutines.withContext
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

/**
 * Creates a router that can handle a specific type of route.
 *
 * @param stackNavigation The stack navigation instance.
 * @param popCallback The callback to pop the route.
 * @param fallBackRouter The fallback router.
 * @return The router.
 */
@Suppress("FunctionName")
inline fun <reified T : Route> AppComponentContext.InnerRouter(
    stackNavigation: StackNavigation<T>,
    fallBackRouter: Router = router,
    noinline popCallback: ((onComplete: (Boolean) -> Unit) -> Unit) =
        { onComplete -> stackNavigation.pop(onComplete) },
): Router = object : Router {

    override fun push(route: Route, onComplete: (Boolean) -> Unit) {
        if (route is T) {
            stackNavigation.pushNew(route, onComplete)
        } else {
            fallBackRouter.push(route, onComplete)
        }
    }

    override fun replaceCurrent(route: Route, onComplete: (Boolean) -> Unit) {
        if (route is T) {
            stackNavigation.replaceCurrent(route, { onComplete(true) })
        } else {
            fallBackRouter.replaceCurrent(route, onComplete)
        }
    }

    override fun replaceAll(vararg routes: Route, onComplete: (Boolean) -> Unit) {
        if (routes.any { it is T }) {
            val newRoutes = routes.toList().filterIsInstance<T>()

            stackNavigation.navigate(
                transformer = { newRoutes },
                onComplete = { newStack, _ -> onComplete(newStack == newRoutes) },
            )
        } else {
            fallBackRouter.replaceAll(*routes, onComplete = onComplete)
        }
    }

    override fun pop(onComplete: (Boolean) -> Unit) {
        popCallback(onComplete)
    }

    override fun popTo(route: Route, onComplete: (Boolean) -> Unit) {
        if (route is T) {
            stackNavigation.navigate(
                transformer = { stack ->
                    stack
                        .dropLastWhile { it != route }
                        .ifEmpty { stack }
                },
                onComplete = { newStack, oldStack -> onComplete(newStack.size < oldStack.size) },
            )
        } else {
            fallBackRouter.popTo(route, onComplete)
        }
    }

    override fun popTo(routeClass: KClass<out Route>, onComplete: (Boolean) -> Unit) {
        if (routeClass == T::class) {
            stackNavigation.navigate(
                transformer = { stack ->
                    stack
                        .dropLastWhile { it::class != routeClass }
                        .ifEmpty { stack }
                },
                onComplete = { newStack, oldStack -> onComplete(newStack.size < oldStack.size) },
            )
        } else {
            fallBackRouter.popTo(routeClass, onComplete)
        }
    }

    override fun <R : Route> popToMatching(
        routeClass: KClass<R>,
        predicate: (R) -> Boolean,
        onComplete: (Boolean) -> Unit,
    ) {
        if (T::class.java.isAssignableFrom(routeClass.java)) {
            val matches: (Route) -> Boolean = { route -> routeClass.safeCast(route)?.let(predicate) == true }

            stackNavigation.navigate(
                transformer = { stack -> stack.dropLastWhile { !matches(it) }.ifEmpty { stack } },
                onComplete = { newStack, _ -> onComplete(newStack.lastOrNull()?.let(matches) == true) },
            )
        } else {
            fallBackRouter.popToMatching(routeClass, predicate, onComplete)
        }
    }

    override suspend fun contains(route: Route): Boolean {
        if (route !is T) return fallBackRouter.contains(route)

        return withContext(dispatchers.mainImmediate) { route in stackNavigation.stack() }
    }
}