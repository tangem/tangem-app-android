package com.tangem.features.send.api.navigation

/**
 * Tracks the previously active route of a send-flow child stack to detect returns from edit screens.
 *
 * A Confirm screen receives the parent's state snapshot via its constructor only when it is freshly

 * Confirm instance is reused, so the flow component must re-push the parent's current state into it.
 * The gate for that re-push must read the route that was active *before* the current one — the
 * Confirm route itself always has `isEditMode = false` ([REDACTED_TASK_KEY]).
 *
 * @param isEditRoute returns whether the given route is an edit screen route
 */
class EditReturnTracker<R : Any>(private val isEditRoute: (R) -> Boolean) {

    private var previousRoute: R? = null

    /**
     * Registers [route] as the currently active route and returns `true` if the route
     * that was active before it was an edit route.
     */
    fun onRouteActivated(route: R): Boolean {
        val isReturnedFromEdit = previousRoute?.let(isEditRoute) == true
        previousRoute = route
        return isReturnedFromEdit
    }
}