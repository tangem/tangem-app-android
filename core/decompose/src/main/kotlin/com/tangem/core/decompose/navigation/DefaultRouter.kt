package com.tangem.core.decompose.navigation

import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
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
 * Router of the root stack. Every operation is logged and runs on the main thread; a navigation failure is logged and
 * reported as `onComplete(false)` instead of being thrown.
 * A stack with a duplicate or no route is never handed to Decompose: its event relay fails permanently on the first
 * throw, so every later navigation (even reading the stack) would crash.
 */
internal class DefaultRouter(
    private val navigationProvider: AppNavigationProvider,
    private val dispatchers: CoroutineDispatcherProvider,
    private val navigationFailureHandler: NavigationFailureHandler,
) : Router, InstanceKeeper.Instance {

    private val logger = TangemLogger.withTag("Router")

    private val navigation: StackNavigation<Route>
        get() = navigationProvider.getOrCreate()

    override fun push(route: Route, onComplete: (isSuccess: Boolean) -> Unit) {
        logger.i("Push $route")
        navigateSafely(withDefaultCompletion(onComplete) { "Unable to push $route" }) { complete ->
            pushOrPopTo(route, complete)
        }
    }

    override fun replaceCurrent(route: Route, onComplete: (Boolean) -> Unit) {
        logger.i("Replace a current route with $route")
        val onCompleteOrReport = withDefaultCompletion(onComplete) { "Unable to replace a current route with $route" }

        navigateSafely(onCompleteOrReport) { complete ->
            navigateValidated(
                transformer = { stack -> stack.dropLast(1) + route },
                onComplete = { newStack, _ -> complete(newStack.lastOrNull() == route) },
            )
        }
    }

    override fun replaceAll(vararg routes: Route, onComplete: (isSuccess: Boolean) -> Unit) {
        val newRoutes = routes.toList()
        logger.i("Replace all routes with ${newRoutes.ifEmpty { "<empty>" }}")

        navigateSafely(withDefaultCompletion(onComplete) { "Unable to replace routes with $newRoutes" }) { complete ->
            navigateValidated(
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

    override fun <R : Route> popToMatching(
        routeClass: KClass<R>,
        predicate: (R) -> Boolean,
        onComplete: (isSuccess: Boolean) -> Unit,
    ) {
        logger.i("Pop to the last matching $routeClass")
        val matches: (Route) -> Boolean = { route -> routeClass.safeCast(route)?.let(predicate) == true }
        navigateSafely(onComplete) { complete ->
            navigate(
                transformer = { stack -> stack.dropLastWhile { !matches(it) }.ifEmpty { stack } },
                onComplete = { newStack, _ -> complete(newStack.lastOrNull()?.let(matches) == true) },
            )
        }
    }

    override fun navigate(transformer: (stack: List<Route>) -> List<Route>, onComplete: (isSuccess: Boolean) -> Unit) {
        navigateSafely(withDefaultCompletion(onComplete) { "Unable to rebuild the stack" }) { complete ->
            var rebuiltStack: List<Route>? = null
            navigateValidated(
                transformer = { stack ->
                    transformer(stack).also { newStack ->
                        rebuiltStack = newStack
                        logger.i("Rebuild the stack to $newStack")
                    }
                },
                onComplete = { newStack, _ -> complete(newStack == rebuiltStack) },
            )
        }
    }

    override fun defaultCompletionHandler(isSuccess: Boolean, errorMessage: String) {
        if (!isSuccess) navigationFailureHandler.onNavigationFailed(errorMessage)
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
                onComplete = { newStack, _ -> complete(newStack.lastOrNull()?.let { !predicate(it) } == true) },
            )
        }
    }

    private fun StackNavigation<Route>.pushOrPopTo(route: Route, onComplete: (isSuccess: Boolean) -> Unit) {
        navigateValidated(
            transformer = { stack -> if (route in stack) stack.dropLastWhile { it != route } else stack + route },
            onComplete = { newStack, oldStack -> onComplete(newStack != oldStack && newStack.lastOrNull() == route) },
        )
    }

    private fun StackNavigation<Route>.navigateValidated(
        transformer: (stack: List<Route>) -> List<Route>,
        onComplete: (newStack: List<Route>, oldStack: List<Route>) -> Unit,
    ) {
        navigate(
            transformer = { stack ->
                val newStack = transformer(stack)
                if (newStack.isNotEmpty() && newStack.toSet().size == newStack.size) {
                    newStack
                } else {
                    logger.e("Rejected invalid stack $newStack, keeping $stack")
                    stack
                }
            },
            onComplete = onComplete,
        )
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
                if (isCompleted) return@onFailure

                complete(false)
                // Popping onto a stack it can't shrink is no failure, but a throw always is
                if (onComplete === Router.NoCompletion) defaultCompletionHandler(false, "Navigation failed: $throwable")
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