package com.tangem.common.routing

import kotlin.reflect.KClass

/**
 * Interface for a router in the application.
 * It provides methods for navigating through the application.
 *
 * Same as [com.tangem.core.decompose.navigation.Router] but without Decompose dependency.
 *
 * ***Must be removed after Decompose migration.***
 */
interface AppRouter {

    /**
     * The current navigation stack.
     */
    val stack: List<AppRoute>

    /**
     * Pushes a new route to the navigation stack.
     *
     * @param route The route to push.
     * @param onComplete The callback to be invoked when the operation is complete.
     */
    fun push(
        route: AppRoute,
        onComplete: (isSuccess: Boolean) -> Unit = { isSuccess ->
            defaultCompletionHandler(isSuccess, errorMessage = "Unable to push $route")
        },
    )

    /**
     * Replaces ***all*** routes in the navigation stack with the specified [routes].
     *
     * @param routes The routes to replace the current stack with.
     * @param onComplete The callback to be invoked when the operation is complete.
     */
    fun replaceAll(
        vararg routes: AppRoute,
        onComplete: (isSuccess: Boolean) -> Unit = { isSuccess ->
            defaultCompletionHandler(isSuccess, errorMessage = "Unable to replace routes with $routes")
        },
    )

    /**
     * Pops the top route from the navigation stack.
     *
     * @param onComplete The callback to be invoked when the operation is complete.
     */
    fun pop(
        onComplete: (isSuccess: Boolean) -> Unit = { isSuccess ->
            defaultCompletionHandler(isSuccess, errorMessage = "Unable to pop route")
        },
    )

    /**
     * Pops routes from the navigation stack until the specified [route] is found.
     *
     * @param route The route to pop to.
     * @param onComplete The callback to be invoked when the operation is complete.
     */
    fun popTo(
        route: AppRoute,
        onComplete: (isSuccess: Boolean) -> Unit = { isSuccess ->
            defaultCompletionHandler(isSuccess, errorMessage = "Unable to pop to $route")
        },
    )

    /**
     * Pops routes from the navigation stack until the ***first*** specified [routeClass] is found.
     *
     * @param routeClass The route class to pop to.
     * @param onComplete The callback to be invoked when the operation is complete.
     */
    fun popTo(
        routeClass: KClass<out AppRoute>,
        onComplete: (isSuccess: Boolean) -> Unit = { isSuccess ->
            defaultCompletionHandler(isSuccess, errorMessage = "Unable to pop to $routeClass")
        },
    )

    fun defaultCompletionHandler(isSuccess: Boolean, errorMessage: String)
}