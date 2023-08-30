package com.tangem.core.navigation

/**
 * Navigation controller that based on redux actions
 *
 * @author Andrew Khokhlov on 10/07/2023
 */
interface ReduxNavController {

    /** Navigate by [action] */
    fun navigate(action: NavigationAction)

    fun getBackStack(): List<AppScreen>
}
