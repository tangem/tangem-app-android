package com.tangem.core.decompose.navigation

import kotlin.reflect.KClass

/**
 * Interface for a router in the application.
 * It provides methods for navigating through the application by stack.
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
     * Replaces a current route with a new one.
     *
     * @param route The route to replace a current one.
     * @param onComplete The callback to be invoked when the operation is complete.
     */
    fun replaceCurrent(route: Route, onComplete: (isSuccess: Boolean) -> Unit = {})

    /**
     * Replaces ***all*** routes in the navigation stack with the specified [routes].
     *
     * @param routes The routes to replace the current stack with.
     * @param onComplete The callback to be invoked when the operation is complete.
     */
    fun replaceAll(vararg routes: Route, onComplete: (isSuccess: Boolean) -> Unit = {})

    /**
     * Pops the top route from the navigation stack.
     *
     * @param onComplete The callback to be invoked when the operation is complete.
     */
    fun pop(onComplete: (isSuccess: Boolean) -> Unit = {})

    /**
     * Pops routes from the navigation stack until the specified [route] is found.
     *
     * @param route The route to pop to.
     * @param onComplete The callback to be invoked when the operation is complete.
     */
    fun popTo(route: Route, onComplete: (isSuccess: Boolean) -> Unit = {})

    /**
     * Pops routes from the navigation stack until the ***first*** specified [routeClass] is found.
     *
     * @param routeClass The route class to pop to.
     * @param onComplete The callback to be invoked when the operation is complete.
     */
    fun popTo(routeClass: KClass<out Route>, onComplete: (isSuccess: Boolean) -> Unit = {})

    /**
     * Pops routes from the navigation stack until the ***last*** route of [routeClass] that satisfies [predicate].
     * Succeeds when such a route ends up on top, including when it already was.
     *
     * @param routeClass The class of the route to pop to.
     * @param predicate The condition the route to pop to must satisfy.
     * @param onComplete The callback to be invoked when the operation is complete.
     */
    fun <R : Route> popToMatching(
        routeClass: KClass<R>,
        predicate: (R) -> Boolean,
        onComplete: (isSuccess: Boolean) -> Unit = {},
    )

    /**
     * Checks whether the [route] is in the navigation stack, including the stacks this router falls back to.
     *
     * @param route The route to look for.
     */
    suspend fun contains(route: Route): Boolean
}