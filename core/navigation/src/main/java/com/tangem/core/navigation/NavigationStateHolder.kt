package com.tangem.core.navigation

/**
 * Navigation state holder
 *
 * @author Andrew Khokhlov on 10/07/2023
 */
interface NavigationStateHolder {

    /** Navigate by [action] */
    fun navigate(action: NavigationAction)
}
