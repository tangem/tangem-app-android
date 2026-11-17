package com.tangem.features.earn.impl

import com.tangem.core.decompose.navigation.Route
import com.tangem.core.decompose.navigation.Router

/**
 * Puts [replacement] in place of the redirecting screen's own [current] route, wherever it sits in the stack.
 *
 * A screen keeps observing its product while a flow is pushed on top of it, so `replaceCurrent` would replace that
 * flow instead and leave the redirecting screen underneath. When [replacement] is already in the stack, [current] is
 * dropped instead, as the stack cannot hold the same route twice.
 */
internal fun Router.redirect(current: Route, replacement: Route) {
    navigate(transformer = { stack -> stack.redirected(current, replacement) })
}

internal fun List<Route>.redirected(current: Route, replacement: Route): List<Route> {
    val index = lastIndexOf(current)
    return when {
        index == -1 -> this
        replacement in this -> filterIndexed { i, _ -> i != index }
        else -> toMutableList().apply { set(index, replacement) }
    }
}