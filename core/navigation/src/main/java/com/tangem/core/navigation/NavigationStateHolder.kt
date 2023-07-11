package com.tangem.core.navigation

/**
 * Navigation state holder
 *
[REDACTED_AUTHOR]
 */
interface NavigationStateHolder {

    /** Navigate by [action] */
    fun navigate(action: NavigationAction)
}