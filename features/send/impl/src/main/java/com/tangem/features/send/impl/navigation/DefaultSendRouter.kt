package com.tangem.features.send.impl.navigation

import androidx.fragment.app.Fragment
import com.tangem.core.navigation.NavigationAction
import com.tangem.core.navigation.ReduxNavController
import com.tangem.features.send.impl.presentation.SendFragment

internal class DefaultSendRouter(
    private val reduxNavController: ReduxNavController,
) : InnerSendRouter {

    override fun getEntryFragment(): Fragment = SendFragment.create()

    override fun openUrl(url: String) {
        reduxNavController.navigate(NavigationAction.OpenUrl(url = url))
    }
}
