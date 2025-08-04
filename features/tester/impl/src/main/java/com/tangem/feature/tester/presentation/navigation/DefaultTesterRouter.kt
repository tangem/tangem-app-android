package com.tangem.feature.tester.presentation.navigation

import androidx.navigation.NavController
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

/**
 * Implementation of router for tester feature
 *
[REDACTED_AUTHOR]
 */
@ActivityScoped
internal class DefaultTesterRouter @Inject constructor() : InnerTesterRouter {

    private var navController: NavController? = null

    override fun setNavController(navController: NavController) {
        this.navController = navController
    }

    override fun open(screen: TesterScreen) {
        navController?.navigate(screen.name)
    }

    override fun back() {
        navController?.popBackStack()
    }
}