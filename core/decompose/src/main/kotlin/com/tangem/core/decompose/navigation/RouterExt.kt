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
