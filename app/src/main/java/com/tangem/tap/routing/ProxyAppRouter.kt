package com.tangem.tap.routing

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.decompose.navigation.Router
import com.tangem.tap.routing.configurator.AppRouterConfig
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.wallet.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.reflect.KClass

internal class ProxyAppRouter(
    private val config: AppRouterConfig,
    private val dispatchers: CoroutineDispatcherProvider,
) : AppRouter {

    private val routerScope: CoroutineScope
        get() = requireNotNull(config.routerScope) {
            "Router scope is not set in config"
        }

    private val innerRouter: Router
        get() = requireNotNull(config.componentRouter) {
            "Inner router is not set in config"
        }

    override val stack: List<AppRoute>
        get() = requireNotNull(config.stack) {
            "Stack is not set in config"
        }

    override fun push(route: AppRoute, onComplete: (isSuccess: Boolean) -> Unit) {
        safeNavigate(onComplete, message = "Push $route") {
            innerRouter.push(route, onComplete)
        }
    }

    override fun replaceAll(vararg routes: AppRoute, onComplete: (isSuccess: Boolean) -> Unit) {
        safeNavigate(onComplete, message = "Replace all routes with $routes") {
            innerRouter.replaceAll(*routes, onComplete = onComplete)
        }
    }

    override fun pop(onComplete: (isSuccess: Boolean) -> Unit) {
        safeNavigate(onComplete, message = "Pop route") {
            innerRouter.pop(onComplete)
        }
    }

    override fun popTo(route: AppRoute, onComplete: (isSuccess: Boolean) -> Unit) {
        safeNavigate(onComplete, message = "Pop to $route") {
            innerRouter.popTo(route, onComplete)
        }
    }

    override fun popTo(routeClass: KClass<out AppRoute>, onComplete: (isSuccess: Boolean) -> Unit) {
        safeNavigate(onComplete, message = "Pop to $routeClass") {
            innerRouter.popTo(routeClass, onComplete)
        }
    }

    private fun safeNavigate(onComplete: (isSuccess: Boolean) -> Unit, message: String, block: () -> Unit) {
        routerScope.launch(dispatchers.mainImmediate) {
            Timber.i(message)

            try {
                block()
            } catch (e: Throwable) {
                onComplete(false)
            }
        }
    }

    override fun defaultCompletionHandler(isSuccess: Boolean, errorMessage: String) {
        if (!isSuccess) {
            FirebaseCrashlytics.getInstance().recordException(RuntimeException(errorMessage))
            Timber.w(errorMessage)

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
