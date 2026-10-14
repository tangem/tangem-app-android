package com.tangem.core.decompose.navigation

import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.router.stack.replaceCurrent
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.Runnable
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KClass

/**
 * Router of the root stack. Every operation runs on the main thread; a navigation failure is logged and reported as
 * `onComplete(false)` instead of being thrown.
 */
internal class DefaultRouter(
    private val navigationProvider: AppNavigationProvider,
    private val dispatchers: CoroutineDispatcherProvider,
) : Router, InstanceKeeper.Instance {

    private val logger = TangemLogger.withTag("Router")

    private val navigation: StackNavigation<Route>
        get() = navigationProvider.getOrCreate()

    override fun push(route: Route, onComplete: (isSuccess: Boolean) -> Unit) {
        navigate(onComplete) { navigation.pushNew(route, onComplete) }
    }

    override fun replaceCurrent(route: Route, onComplete: (Boolean) -> Unit) {
        navigate(onComplete) { navigation.replaceCurrent(route, { onComplete(true) }) }
    }

    override fun replaceAll(vararg routes: Route, onComplete: (isSuccess: Boolean) -> Unit) {
        val newRoutes = routes.toList()

        navigate(onComplete) {
            navigation.navigate(
                transformer = { newRoutes },
                onComplete = { newStack, _ -> onComplete(newStack == newRoutes) },
            )
        }
    }

    override fun pop(onComplete: (isSuccess: Boolean) -> Unit) {
        navigate(onComplete) { navigation.pop(onComplete) }
    }

    override fun popTo(route: Route, onComplete: (isSuccess: Boolean) -> Unit) {
        popTo(
            predicate = { it != route },
            onComplete = onComplete,
        )
    }

    override fun popTo(routeClass: KClass<out Route>, onComplete: (isSuccess: Boolean) -> Unit) {
        popTo(
            predicate = { it::class != routeClass },
            onComplete = onComplete,
        )
    }

    private fun popTo(predicate: (Route) -> Boolean, onComplete: (isSuccess: Boolean) -> Unit) {
        navigate(onComplete) {
            navigation.navigate(
                transformer = { stack ->
                    stack
                        .dropLastWhile(predicate)
                        .ifEmpty { stack }
                },
                onComplete = { newStack, oldStack -> onComplete(newStack.size < oldStack.size) },
            )
        }
    }

    private fun navigate(onComplete: (isSuccess: Boolean) -> Unit, block: () -> Unit) {
        val safeBlock = Runnable {
            runCatching(block).onFailure { throwable ->
                logger.e(messageString = "Navigation failed", throwable = throwable)
                onComplete(false)
            }
        }

        val mainDispatcher = dispatchers.mainImmediate
        if (mainDispatcher.isDispatchNeeded(EmptyCoroutineContext)) {
            mainDispatcher.dispatch(EmptyCoroutineContext, safeBlock)
        } else {
            safeBlock.run()
        }
    }
}