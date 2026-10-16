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