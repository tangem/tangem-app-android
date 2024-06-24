package com.tangem.core.decompose.navigation

import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.popWhile
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import kotlin.reflect.KClass

internal class DefaultRouter(
    private val navigationProvider: AppNavigationProvider,
) : Router, InstanceKeeper.Instance {

    private val navigation: StackNavigation<Route>
        get() = navigationProvider.getOrCreate()

    @OptIn(ExperimentalDecomposeApi::class)
    override fun push(route: Route, onComplete: (isSuccess: Boolean) -> Unit) {
        navigation.pushNew(route, onComplete)
    }

    override fun replaceAll(vararg routes: Route, onComplete: (isSuccess: Boolean) -> Unit) {
        val newRoutes = routes.toList()

        navigation.navigate(
            transformer = { newRoutes },
            onComplete = { newStack, _ -> onComplete(newStack.size == newRoutes.size) },
        )
    }

    override fun pop(onComplete: (isSuccess: Boolean) -> Unit) {
        navigation.pop(onComplete)
    }

    override fun popTo(route: Route, onComplete: (isSuccess: Boolean) -> Unit) {
        navigation.popWhile(
            predicate = { it != route },
            onComplete = onComplete,
        )
    }

    override fun popTo(routeClass: KClass<out Route>, onComplete: (isSuccess: Boolean) -> Unit) {
        navigation.popWhile(
            predicate = { it::class != routeClass },
            onComplete = onComplete,
        )
    }
}
