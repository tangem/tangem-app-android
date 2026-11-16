package com.tangem.core.decompose.navigation

/**
 * Pops routes from the navigation stack until the specified route [R] is found.
 *
 * @param R The route to pop to.
 * @param onComplete The callback to be invoked when the operation is complete.
 */
inline fun <reified R : Route> Router.popTo(noinline onComplete: (isSuccess: Boolean) -> Unit = Router.NoCompletion) {
    popTo(R::class, onComplete)
}

/**
 * Pops back to the [route] if it is in the navigation stack, otherwise opens it in place of the current route.
 *
 * @param route The route to open.
 */
fun Router.popToOrReplaceCurrent(route: Route) {
    popTo(route) { isFound -> if (!isFound) replaceCurrent(route) }
}

/**
 * Pops back to the last route of type [R] that satisfies [predicate], otherwise opens [route] in place of the current
 * route. Use it instead of the equality-based overload when the route in the stack may differ from [route] in fields
 * that don't identify the screen.
 *
 * @param route The route to open when no matching route is in the stack.
 * @param predicate The condition a route in the stack must satisfy to be popped back to.
 */
inline fun <reified R : Route> Router.popToOrReplaceCurrent(route: R, noinline predicate: (R) -> Boolean) {
    popToMatching(R::class, predicate) { isFound -> if (!isFound) replaceCurrent(route) }
}

/**
 * Returns [onComplete], or a callback that hands the result to [Router.defaultCompletionHandler] when the caller passed
 * [Router.NoCompletion].
 *
 * @param onComplete The callback the caller passed.
 * @param errorMessage Describes the operation if it fails.
 */
/** Pushes [route] unless it is already in the stack. */
fun Router.pushIfAbsent(route: Route, onComplete: (isSuccess: Boolean) -> Unit = Router.NoCompletion) {
    navigate(transformer = { stack -> if (route in stack) stack else stack + route }, onComplete = onComplete)
}

/** Brings [route] to the top of the stack, see [List.pushOrReplaceCurrent]. */
fun Router.pushOrReplaceCurrent(route: Route, onComplete: (isSuccess: Boolean) -> Unit = Router.NoCompletion) {
    navigate(transformer = { stack -> stack.pushOrReplaceCurrent(route) }, onComplete = onComplete)
}

/**
 * Pops back to [route] when the stack holds it, otherwise replaces a current route of the same class with it or
 * pushes it. Any route of that class on top must be safe to close.
 */
fun List<Route>.pushOrReplaceCurrent(route: Route): List<Route> = when {
    route in this -> dropLastWhile { it != route }
    lastOrNull()?.let { it::class } == route::class -> dropLast(1) + route
    else -> this + route
}

fun Router.withDefaultCompletion(
    onComplete: (isSuccess: Boolean) -> Unit,
    errorMessage: () -> String,
): (isSuccess: Boolean) -> Unit {
    if (onComplete !== Router.NoCompletion) return onComplete

    return { isSuccess -> defaultCompletionHandler(isSuccess, errorMessage()) }
}