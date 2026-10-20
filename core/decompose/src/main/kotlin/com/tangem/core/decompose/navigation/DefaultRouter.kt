package com.tangem.core.decompose.navigation

import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.router.stack.replaceCurrent
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.tangem.core.decompose.utils.stack
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.withContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

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
        navigateSafely(onComplete) { complete -> pushNew(route, complete) }
    }

    override fun replaceCurrent(route: Route, onComplete: (Boolean) -> Unit) {
        navigateSafely(onComplete) { complete -> this.replaceCurrent(route, { complete(true) }) }
    }

    override fun replaceAll(vararg routes: Route, onComplete: (isSuccess: Boolean) -> Unit) {
        val newRoutes = routes.toList()

        navigateSafely(onComplete) { complete ->
            navigate(
                transformer = { newRoutes },
                onComplete = { newStack, _ -> complete(newStack == newRoutes) },
            )
        }
    }

    override fun pop(onComplete: (isSuccess: Boolean) -> Unit) {
        navigateSafely(onComplete) { complete -> this.pop(complete) }
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

    override fun <R : Route> popToMatching(
        routeClass: KClass<R>,
        predicate: (R) -> Boolean,
        onComplete: (isSuccess: Boolean) -> Unit,
    ) {
        popTo(
            predicate = { route -> routeClass.safeCast(route)?.let(predicate) != true },
            onComplete = onComplete,
        )
    }

    override suspend fun contains(route: Route): Boolean = withContext(dispatchers.mainImmediate) {
        route in navigation.stack()
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