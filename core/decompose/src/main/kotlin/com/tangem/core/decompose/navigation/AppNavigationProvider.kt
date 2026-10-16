@file:Suppress("UNCHECKED_CAST")

package com.tangem.core.decompose.navigation

import com.arkivanov.decompose.router.stack.StackNavigation

/**
 * Interface for providing application navigation.
 * It provides or creates a StackNavigation instance for the application.
 */
interface AppNavigationProvider {

    /** Routes of the stack driven by [getOrCreate], as last reported through [updateStack]. */
    val stack: List<Route>

    /**
     * Gets or creates a StackNavigation instance.
     *
     * @return The StackNavigation instance.
     */
    fun getOrCreate(): StackNavigation<Route>

    /**
     * Reports the current routes of the stack driven by [getOrCreate]. Called by the component that owns the stack.
     *
     * @param stack The current routes, from the bottom to the top.
     */
    fun updateStack(stack: List<Route>)
}

/**
 * Gets or creates a [StackNavigation] instance of a specific type.
 *
 * @return The [StackNavigation] instance.
 */
fun <R : Route> AppNavigationProvider.getOrCreateTyped(): StackNavigation<R> {
    return getOrCreate() as StackNavigation<R>
}