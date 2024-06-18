package com.tangem.core.decompose.navigation

/**
 * Interface for owning navigation-related properties.
 */
interface NavigationOwner {

    /**
     * The [Router] instance.
     */
    val router: Router

    /**
     * The [AppNavigationProvider] instance.
     */
    val navigationProvider: AppNavigationProvider
}