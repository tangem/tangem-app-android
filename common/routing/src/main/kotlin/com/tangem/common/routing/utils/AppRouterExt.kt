package com.tangem.common.routing.utils

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter

/**
 * Pops routes from the navigation stack until the specified route [R] is found.
 *
 * ***Must be removed after Decompose migration.***
 *
 * @param R The route to pop to.
 * @param onComplete The callback to be invoked when the operation is complete.
 */
inline fun <reified R : AppRoute> AppRouter.popTo(noinline onComplete: (isSuccess: Boolean) -> Unit = {}) {
    popTo(R::class, onComplete)
}

/**
 * Pushes [route] only if an equal route is not already present in the navigation [stack].
 *
 * Re-pushing a route that already exists in the stack is reported by Decompose as a failed
 * navigation — a no-op when the route is on top, an exception when it is deeper — which surfaces a
 * generic error snackbar. Since the destination is already open, the push is skipped instead. Useful
 * for re-entrant entry points such as deeplinks, where the same route may be requested twice.
 *
 * ***Must be removed after Decompose migration.***
 *
 * @param route The route to push.
 */
fun AppRouter.pushIfNotInStack(route: AppRoute) {
    if (stack.contains(route)) return
    push(route)
}