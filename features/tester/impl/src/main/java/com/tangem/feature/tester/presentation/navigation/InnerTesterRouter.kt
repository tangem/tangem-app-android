package com.tangem.feature.tester.presentation.navigation

import androidx.navigation.NavController

/**
 * Inner feature router
 *
[REDACTED_AUTHOR]
 */
internal interface InnerTesterRouter {

    /** Set up a navigation controller that bound to tester navigation graph */
    fun setNavController(navController: NavController)

    /** Open specified screen [screen] */
    fun open(screen: TesterScreen)

    /** Open last screen */
    fun back()
}