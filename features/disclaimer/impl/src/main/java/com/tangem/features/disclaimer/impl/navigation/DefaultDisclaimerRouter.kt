package com.tangem.features.disclaimer.impl.navigation

import androidx.fragment.app.Fragment
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.core.navigation.ReduxNavController
import com.tangem.features.disclaimer.impl.DisclaimerFragment
import javax.inject.Inject

internal class DefaultDisclaimerRouter @Inject constructor(
    private val reduxNavController: ReduxNavController,
) : InnerDisclaimerRouter {

    override fun entryFragment(): Fragment = DisclaimerFragment.create()

    override fun openPushNotificationPermission() {
        // todo https://tangem.atlassian.net/browse/AND-7355
    }

    override fun openHome() {
        reduxNavController.navigate(action = NavigationAction.NavigateTo(screen = AppScreen.Home))
    }
}
