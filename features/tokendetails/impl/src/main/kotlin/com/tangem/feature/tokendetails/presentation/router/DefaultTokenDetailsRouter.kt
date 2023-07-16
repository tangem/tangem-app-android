package com.tangem.feature.tokendetails.presentation.router

import androidx.fragment.app.Fragment
import com.tangem.core.navigation.NavigationAction
import com.tangem.core.navigation.NavigationStateHolder
import com.tangem.feature.tokendetails.presentation.TokenDetailsFragment

internal class DefaultTokenDetailsRouter(
    private val navigationStateHolder: NavigationStateHolder,
) : InnerTokenDetailsRouter {

    override fun getEntryFragment(): Fragment = TokenDetailsFragment()

    override fun popBackStack() {
        navigationStateHolder.navigate(NavigationAction.PopBackTo())
    }
}
