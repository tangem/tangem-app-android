package com.tangem.tap.routing

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.decompose.navigation.Router
import com.tangem.tap.routing.configurator.AppRouterConfig
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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
        routerScope.launch(dispatchers.mainImmediate) {
            innerRouter.push(route, onComplete)
        }
    }

    override fun replaceAll(vararg routes: AppRoute, onComplete: (isSuccess: Boolean) -> Unit) {
        routerScope.launch(dispatchers.mainImmediate) {
            innerRouter.replaceAll(*routes, onComplete = onComplete)
        }
    }

    override fun pop(onComplete: (isSuccess: Boolean) -> Unit) {
        routerScope.launch(dispatchers.mainImmediate) {
            innerRouter.pop(onComplete)
        }
    }

    override fun popTo(route: AppRoute, onComplete: (isSuccess: Boolean) -> Unit) {
        routerScope.launch(dispatchers.mainImmediate) {
            innerRouter.popTo(route, onComplete)
        }
    }

    override fun popTo(routeClass: KClass<out AppRoute>, onComplete: (isSuccess: Boolean) -> Unit) {
        routerScope.launch(dispatchers.mainImmediate) {
            innerRouter.popTo(routeClass, onComplete)
        }
    }
}
