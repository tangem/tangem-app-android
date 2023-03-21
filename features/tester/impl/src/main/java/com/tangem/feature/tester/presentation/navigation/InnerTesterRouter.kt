package com.tangem.feature.tester.presentation.navigation

import androidx.navigation.NavController
import com.tangem.features.tester.api.TesterRouter

/**
 * Inner feature router
 *
 * @author Andrew Khokhlov on 07/02/2023
 */
internal interface InnerTesterRouter : TesterRouter {

    /** Set up a navigation controller that bound to tester navigation graph */
    fun setNavController(navController: NavController)

    /** Open specified screen [screen] */
    fun open(screen: TesterScreen)

    /** Open last screen */
    fun back()
}
