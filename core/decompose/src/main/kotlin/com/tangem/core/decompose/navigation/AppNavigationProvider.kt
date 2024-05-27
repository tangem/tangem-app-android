@file:Suppress("UNCHECKED_CAST")

package com.tangem.core.decompose.navigation

import com.arkivanov.decompose.router.stack.StackNavigation

/**
 * Interface for providing application navigation.
 * It provides or creates a StackNavigation instance for the application.
 */
interface AppNavigationProvider {

    /**
     * Gets or creates a StackNavigation instance.
     *
     * @return The StackNavigation instance.
     */
    fun getOrCreate(): StackNavigation<Route>
}

/**
 * Gets or creates a [StackNavigation] instance of a specific type.
 *
 * @return The [StackNavigation] instance.
 */
fun <R : Route> AppNavigationProvider.getOrCreateTyped(): StackNavigation<R> {
    return getOrCreate() as StackNavigation<R>
}