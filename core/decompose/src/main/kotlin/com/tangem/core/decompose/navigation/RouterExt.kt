package com.tangem.core.decompose.navigation

/**
 * Pops routes from the navigation stack until the specified route [R] is found.
 *
 * @param R The route to pop to.
 * @param onComplete The callback to be invoked when the operation is complete.
 */
inline fun <reified R : Route> Router.popTo(noinline onComplete: (isSuccess: Boolean) -> Unit = {}) {
    popTo(R::class, onComplete)
}

/**
 * Pops back to the [route] if it is in the navigation stack, otherwise opens it in place of the current route.
 *
 * @param route The route to open.
 */
fun Router.popToOrReplaceCurrent(route: Route) {
    popTo(route) { isFound -> if (!isFound) replaceCurrent(route) }
}

/**
 * Pops back to the last route of type [R] that satisfies [predicate], otherwise opens [route] in place of the current
 * route. Use it instead of the equality-based overload when the route in the stack may differ from [route] in fields
 * that don't identify the screen.
 *
 * @param route The route to open when no matching route is in the stack.
 * @param predicate The condition a route in the stack must satisfy to be popped back to.
 */
inline fun <reified R : Route> Router.popToOrReplaceCurrent(route: R, noinline predicate: (R) -> Boolean) {
    popToMatching(R::class, predicate) { isFound -> if (!isFound) replaceCurrent(route) }
}