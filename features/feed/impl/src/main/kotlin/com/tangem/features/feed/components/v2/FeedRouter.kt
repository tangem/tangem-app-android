package com.tangem.features.feed.components.v2

import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.features.feed.nav.FeedRoute
import com.tangem.utils.logging.TangemLogger

/**
 * Creates the router of the v2 feed. One handle carries every navigation target a feed screen has;
 * `push` dispatches by route type:
 *
 * - not a [FeedRoute] (e.g. an `AppRoute`) → [InnerRouter] falls through to the parent context's
 * router, so global destinations open above the feed host;
 * - a [FeedRoute] with no registered screen → error log + no-op. Never forwarded to the global
 * router: nothing above the feed can resolve it either, forwarding would only relocate the crash;
 * - [FeedRoute.isSingleInstance] → brings an already-present route to front instead of stacking
 * a duplicate (markets list ↔ search cycles);
 * - otherwise → a new entry on the feed stack.
 *
 * Pop policy stays with the host: [InnerRouter] invokes [popCallback] on every `pop()` regardless
 * of the stack state, so the host owns the "root with empty back stack → pop globally" decision.
 */
@Suppress("FunctionName")
internal fun AppComponentContext.FeedRouter(
    stackNavigation: StackNavigation<FeedRoute>,
    isRouteRegistered: (FeedRoute) -> Boolean,
    popCallback: (onComplete: (Boolean) -> Unit) -> Unit,
): Router {
    val innerRouter = InnerRouter(
        stackNavigation = stackNavigation,
        popCallback = popCallback,
    )

    return object : Router by innerRouter {

        override fun push(route: Route, onComplete: (Boolean) -> Unit) {
            when {
                route !is FeedRoute -> innerRouter.push(route, onComplete)
                !isRouteRegistered(route) -> {
                    TangemLogger.e("No feed screen registered for $route")
                    onComplete(false)
                }
                route.isSingleInstance -> stackNavigation.bringToFront(route) { onComplete(true) }
                else -> innerRouter.push(route, onComplete)
            }
        }
    }
}