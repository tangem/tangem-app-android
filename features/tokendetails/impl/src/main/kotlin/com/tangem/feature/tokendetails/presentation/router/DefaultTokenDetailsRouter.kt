package com.tangem.feature.tokendetails.presentation.router

import androidx.fragment.app.Fragment
import com.tangem.core.navigation.NavigationAction
import com.tangem.core.navigation.ReduxNavController
import com.tangem.feature.tokendetails.presentation.TokenDetailsFragment

internal class DefaultTokenDetailsRouter(
    private val reduxNavController: ReduxNavController,
) : InnerTokenDetailsRouter {

    override fun getEntryFragment(): Fragment = TokenDetailsFragment()

    override fun popBackStack() {
        reduxNavController.navigate(NavigationAction.PopBackTo())
    }

    override fun openUrl(url: String) {
        reduxNavController.navigate(NavigationAction.OpenUrl(url = url))
    }
}