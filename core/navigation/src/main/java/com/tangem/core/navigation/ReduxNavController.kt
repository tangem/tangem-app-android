package com.tangem.core.navigation

/**
 * Navigation controller that based on redux actions
 *
[REDACTED_AUTHOR]
 */
interface ReduxNavController {

    /** Navigate by [action] */
    fun navigate(action: NavigationAction)

    fun getBackStack(): List<AppScreen>
}