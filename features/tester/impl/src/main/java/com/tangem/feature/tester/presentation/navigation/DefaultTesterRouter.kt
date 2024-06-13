package com.tangem.feature.tester.presentation.navigation

import android.content.Context
import android.content.Intent
import androidx.navigation.NavController
import com.tangem.feature.tester.presentation.TesterActivity
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

/**
 * Implementation of router for tester feature
 *
 * @property context activity context
 *
[REDACTED_AUTHOR]
 */
@ActivityScoped
internal class DefaultTesterRouter @Inject constructor(
    @ActivityContext private val context: Context,
) : InnerTesterRouter {

    private var navController: NavController? = null

    override fun getEntryIntent(): Intent {
        return Intent(context, TesterActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

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