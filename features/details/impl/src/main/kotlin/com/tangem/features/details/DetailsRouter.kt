package com.tangem.features.details

import com.tangem.core.decompose.navigation.Route
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.NavigationAction
import com.tangem.core.navigation.ReduxNavController
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.features.details.routing.DetailsRoute
import com.tangem.features.tester.api.TesterRouter
import javax.inject.Inject

// TODO: Remove after [REDACTED_JIRA]
internal class DetailsRouter @Inject constructor(
    private val reduxNavController: ReduxNavController,
    private val reduxStateHolder: ReduxStateHolder,
    private val testerRouter: TesterRouter,
) : Router {

    override fun push(route: Route, onComplete: (isSuccess: Boolean) -> Unit) {
        if (route is DetailsRoute) {
            when (route) {
                is DetailsRoute.Screen -> {
                    reduxNavController.navigate(NavigationAction.NavigateTo(route.screen, bundle = route.params))
                }
                is DetailsRoute.Feedback -> {
                    reduxStateHolder.sendFeedbackEmail()
                }
                is DetailsRoute.TesterMenu -> {
                    testerRouter.startTesterScreen()
                }
                is DetailsRoute.Url -> {
                    reduxNavController.navigate(NavigationAction.OpenUrl(route.url))
                }
            }
            onComplete(true)
        } else {
            onComplete(false)
        }
    }

    override fun pop(onComplete: (isSuccess: Boolean) -> Unit) {
        reduxNavController.popBackStack()
        onComplete(true)
    }

    override fun popTo(route: Route, onComplete: (isSuccess: Boolean) -> Unit) {
        if (route is DetailsRoute.Screen) {
            reduxNavController.popBackStack(route.screen)
            onComplete(true)
        } else {
            reduxNavController.getBackStack()
            onComplete(false)
        }
    }
}