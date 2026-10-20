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
 * Router of the root stack. Every operation is logged and runs on the main thread; a navigation failure is logged and
 * reported as `onComplete(false)` instead of being thrown.
 */
internal class DefaultRouter(
    private val navigationProvider: AppNavigationProvider,
    private val dispatchers: CoroutineDispatcherProvider,
) : Router, InstanceKeeper.Instance {

    private val logger = TangemLogger.withTag("Router")

    private val navigation: StackNavigation<Route>
        get() = navigationProvider.getOrCreate()

    override fun push(route: Route, onComplete: (isSuccess: Boolean) -> Unit) {
        logger.i("Push $route")
        navigateSafely(onComplete) { complete -> pushNew(route, complete) }
    }

    override fun replaceCurrent(route: Route, onComplete: (Boolean) -> Unit) {
        logger.i("Replace a current route with $route")
        navigateSafely(onComplete) { complete -> this.replaceCurrent(route, { complete(true) }) }
    }

    override fun replaceAll(vararg routes: Route, onComplete: (isSuccess: Boolean) -> Unit) {
        val newRoutes = routes.toList()
        logger.i("Replace all routes with ${newRoutes.ifEmpty { "<empty>" }}")

        navigateSafely(onComplete) { complete ->
            navigate(
                transformer = { newRoutes },
                onComplete = { newStack, _ -> complete(newStack == newRoutes) },
            )
        }
    }

    override fun pop(onComplete: (isSuccess: Boolean) -> Unit) {
        logger.i("Pop route")
        navigateSafely(onComplete) { complete -> this.pop(complete) }
    }

    override fun popTo(route: Route, onComplete: (isSuccess: Boolean) -> Unit) {
        logger.i("Pop to $route")
        popTo(
            predicate = { it != route },
            onComplete = onComplete,
        )
    }

    override fun popTo(routeClass: KClass<out Route>, onComplete: (isSuccess: Boolean) -> Unit) {
        logger.i("Pop to $routeClass")
        popTo(
            predicate = { it::class != routeClass },
            onComplete = onComplete,
        )
    }

    private fun popTo(predicate: (Route) -> Boolean, onComplete: (isSuccess: Boolean) -> Unit) {
        navigateSafely(onComplete) { complete ->
            navigate(
                transformer = { stack ->
                    stack
                        .dropLastWhile(predicate)
                        .ifEmpty { stack }
                },
                onComplete = { newStack, oldStack -> complete(newStack.size < oldStack.size) },
            )
        }
    }

    /**
     * [block] must report its result through the provided callback. A failure is logged; [onComplete] gets `false`
     * only if it hasn't been called yet, so a failure thrown by the callback itself never retries it.
     */
    private fun navigateSafely(
        onComplete: (isSuccess: Boolean) -> Unit,
        block: StackNavigation<Route>.(complete: (Boolean) -> Unit) -> Unit,
    ) {
        val safeBlock = Runnable {
            var isCompleted = false
            val complete: (Boolean) -> Unit = { isSuccess ->
                isCompleted = true
                runCatching { onComplete(isSuccess) }.onFailure { throwable ->
                    logger.e(messageString = "Navigation completion callback failed", throwable = throwable)
                }
            }

            runCatching { navigation.block(complete) }.onFailure { throwable ->
                logger.e(messageString = "Navigation failed", throwable = throwable)
                if (!isCompleted) complete(false)
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