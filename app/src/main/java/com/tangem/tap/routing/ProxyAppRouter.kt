package com.tangem.tap.routing

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.decompose.navigation.Router
import com.tangem.tap.routing.configurator.AppRouterConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

internal class ProxyAppRouter(
    private val configurator: AppRouterConfig,
) : AppRouter {

    private val routerScope: CoroutineScope
        get() = requireNotNull(configurator.routerScope) {
            "Router scope is not set"
        }

    private val innerRouter: Router
        get() = requireNotNull(configurator.componentRouter) {
            "Inner router is not set"
        }

    override val backStack: List<AppRoute>
        get() = requireNotNull(configurator.baclStack) {
            "Inner back stack is not set"
        }

    override fun push(route: AppRoute, onComplete: (isSuccess: Boolean) -> Unit) {
        routerScope.launch(Dispatchers.Main.immediate) {
            innerRouter.push(route, onComplete)
        }
    }

    override fun pop(onComplete: (isSuccess: Boolean) -> Unit) {
        routerScope.launch(Dispatchers.Main.immediate) {
            innerRouter.pop(onComplete)
        }
    }

    override fun popTo(route: AppRoute, onComplete: (isSuccess: Boolean) -> Unit) {
        routerScope.launch(Dispatchers.Main.immediate) {
            innerRouter.popTo(route, onComplete)
        }
    }

    override fun popTo(routeClass: KClass<out AppRoute>, onComplete: (isSuccess: Boolean) -> Unit) {
        routerScope.launch(Dispatchers.Main.immediate) {
            innerRouter.popTo(routeClass, onComplete)
        }
    }

    override fun clear(onComplete: (isSuccess: Boolean) -> Unit) {
        routerScope.launch(Dispatchers.Main.immediate) {
            innerRouter.clear(onComplete)
        }
    }
}
