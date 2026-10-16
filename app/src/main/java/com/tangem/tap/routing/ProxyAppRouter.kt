package com.tangem.tap.routing

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.core.analytics.models.ExceptionAnalyticsEvent
import com.tangem.core.decompose.navigation.Router
import com.tangem.tap.routing.configurator.AppRouterConfig
import com.tangem.utils.logging.TangemLogger
import com.tangem.wallet.R
import kotlin.reflect.KClass

/**
 * Bridges app-level singletons to the root router of the current activity. Main thread and failure handling are
 * provided by the root router itself.
 */
internal class ProxyAppRouter(
    private val config: AppRouterConfig,
    private val analyticsExceptionHandler: AnalyticsExceptionHandler,
) : AppRouter {

    private val logger = TangemLogger.withTag("AppRouter")

    private val innerRouter: Router
        get() = requireNotNull(config.componentRouter) {
            "Inner router is not set in config"
        }

    override val stack: List<AppRoute>
        get() = requireNotNull(config.stack) {
            "Stack is not set in config"
        }

    override fun push(route: AppRoute, onComplete: (isSuccess: Boolean) -> Unit) {
        logger.i("Push $route")
        innerRouter.push(route, onComplete)
    }

    override fun replaceCurrent(route: AppRoute, onComplete: (Boolean) -> Unit) {
        logger.i("Replace a current route with $route")
        innerRouter.replaceCurrent(route, onComplete)
    }

    override fun replaceAll(vararg routes: AppRoute, onComplete: (isSuccess: Boolean) -> Unit) {
        logger.i("Replace all routes with ${routes.toList().ifEmpty { "<empty>" }}")
        innerRouter.replaceAll(*routes, onComplete = onComplete)
    }

    override fun pop(onComplete: (isSuccess: Boolean) -> Unit) {
        logger.i("Pop route")
        innerRouter.pop(onComplete)
    }

    override fun popTo(route: AppRoute, onComplete: (isSuccess: Boolean) -> Unit) {
        logger.i("Pop to $route")
        innerRouter.popTo(route, onComplete)
    }

    override fun popTo(routeClass: KClass<out AppRoute>, onComplete: (isSuccess: Boolean) -> Unit) {
        logger.i("Pop to $routeClass")
        innerRouter.popTo(routeClass, onComplete)
    }

    override fun defaultCompletionHandler(isSuccess: Boolean, errorMessage: String) {
        if (!isSuccess) {
            analyticsExceptionHandler.sendException(ExceptionAnalyticsEvent(RuntimeException(errorMessage)))
            logger.w(errorMessage)

            with(receiver = config.snackbarHandler ?: return) {
                showSnackbar(
                    text = R.string.common_unknown_error,
                    buttonTitle = R.string.common_ok,
                    action = { dismissSnackbar() },
                )
            }
        }
    }
}