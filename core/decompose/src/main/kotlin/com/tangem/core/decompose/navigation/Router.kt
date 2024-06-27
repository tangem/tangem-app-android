package com.tangem.core.decompose.navigation

/**
 * Interface for a router in the application.
 * It provides methods for navigating through the application.
 */
interface Router {

    /**
     * Pushes a new route to the navigation stack.
     *
     * @param route The route to push.
     * @param onComplete The callback to be invoked when the operation is complete.
     */
    fun push(route: Route, onComplete: (isSuccess: Boolean) -> Unit = {})

    /**
     * Pops the top route from the navigation stack.
     *
     * @param onComplete The callback to be invoked when the operation is complete.
     */
    fun pop(onComplete: (isSuccess: Boolean) -> Unit = {})

    /**
     * Pops routes from the navigation stack until the specified route is found.
     *
     * @param route The route to pop to.
     * @param onComplete The callback to be invoked when the operation is complete.
     */
    fun popTo(route: Route, onComplete: (isSuccess: Boolean) -> Unit = {})
}