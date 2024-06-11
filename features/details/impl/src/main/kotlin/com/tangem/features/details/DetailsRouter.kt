package com.tangem.features.details

import com.tangem.core.decompose.navigation.Route
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.NavigationAction
import com.tangem.core.navigation.ReduxNavController
import com.tangem.core.navigation.feedback.FeedbackManager
import com.tangem.core.navigation.feedback.FeedbackType
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.features.details.routing.DetailsRoute
import com.tangem.features.tester.api.TesterRouter
import javax.inject.Inject
import kotlin.reflect.KClass

// TODO: Remove after [REDACTED_JIRA]
internal class DetailsRouter @Inject constructor(
    private val reduxNavController: ReduxNavController,
    private val feedbackManager: FeedbackManager,
    private val testerRouter: TesterRouter,
    private val urlOpener: UrlOpener,
) : Router {

    override fun push(route: Route, onComplete: (isSuccess: Boolean) -> Unit) {
        if (route is DetailsRoute) {
            when (route) {
                is DetailsRoute.Screen -> {
                    reduxNavController.navigate(NavigationAction.NavigateTo(route.screen, bundle = route.params))
                }
                is DetailsRoute.Feedback -> {
                    feedbackManager.sendEmail(FeedbackType.Feedback)
                }
                is DetailsRoute.TesterMenu -> {
                    testerRouter.startTesterScreen()
                }
                is DetailsRoute.Url -> {
                    urlOpener.openUrl(route.url)
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

    override fun popTo(routeClass: KClass<out Route>, onComplete: (isSuccess: Boolean) -> Unit) {
        TODO("This class will be removed in future")
    }

    override fun popTo(route: Route, onComplete: (isSuccess: Boolean) -> Unit) {
        TODO("This class will be removed in future")
    }

    override fun clear(onComplete: (isSuccess: Boolean) -> Unit) {
        TODO("This class will be removed in future")
    }
}