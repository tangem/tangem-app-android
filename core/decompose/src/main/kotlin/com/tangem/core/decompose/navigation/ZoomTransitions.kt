package com.tangem.core.decompose.navigation

/**
 * Routes pushed with [pushZooming], waiting for the stack that shows them to claim their zoom source.
 *
 * Kept beside the route rather than in it: how a screen was opened is not part of what the screen is, so it
 * stays out of the route's equality and out of the saved stack. A route restored after process death simply
 * opens, and closes, with its stack's own animation.
 */
object ZoomTransitions {

    // a push the router turned down is never claimed; the cap keeps those from piling up
    private const val MAX_PENDING = 8

    private val lock = Any()
    private val pending = ArrayDeque<Pair<Route, String>>()

    fun register(route: Route, sourceKey: String) {
        synchronized(lock) {
            pending.removeAll { (pendingRoute, _) -> pendingRoute === route }
            pending.addLast(route to sourceKey)
            if (pending.size > MAX_PENDING) pending.removeFirst()
        }
    }

    /** The zoom source [route] was pushed with, handed out once; matched by identity, not equality. */
    fun claim(route: Route): String? = synchronized(lock) {
        val index = pending.indexOfFirst { (pendingRoute, _) -> pendingRoute === route }
        if (index < 0) null else pending.removeAt(index).second
    }
}

/**
 * Pushes [route] zooming out of the view marked with `Modifier.zoomTransitionSource(sourceKey)`, like an iOS
 * zoom transition. Going back zooms into that view again.
 */
fun Router.pushZooming(
    route: Route,
    sourceKey: String,
    onComplete: (isSuccess: Boolean) -> Unit = Router.NoCompletion,
) {
    ZoomTransitions.register(route = route, sourceKey = sourceKey)
    push(route = route, onComplete = onComplete)
}