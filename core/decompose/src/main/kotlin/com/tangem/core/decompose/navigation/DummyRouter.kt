package com.tangem.core.decompose.navigation

import kotlin.reflect.KClass

class DummyRouter : Router {

    override fun push(route: Route, onComplete: (isSuccess: Boolean) -> Unit) {
        onComplete(true)
    }

    override fun replaceCurrent(route: Route, onComplete: (Boolean) -> Unit) {
        onComplete(true)
    }

    override fun replaceAll(vararg routes: Route, onComplete: (isSuccess: Boolean) -> Unit) {
        onComplete(true)
    }

    override fun pop(onComplete: (isSuccess: Boolean) -> Unit) {
        onComplete(true)
    }

    override fun popTo(route: Route, onComplete: (isSuccess: Boolean) -> Unit) {
        onComplete(true)
    }

    override fun popTo(routeClass: KClass<out Route>, onComplete: (isSuccess: Boolean) -> Unit) {
        onComplete(true)
    }

    override fun <R : Route> popToMatching(
        routeClass: KClass<R>,
        predicate: (R) -> Boolean,
        onComplete: (isSuccess: Boolean) -> Unit,
    ) {
        onComplete(true)
    }

    override fun navigate(transformer: (stack: List<Route>) -> List<Route>, onComplete: (isSuccess: Boolean) -> Unit) {
        onComplete(true)
    }

    override suspend fun contains(route: Route): Boolean = false
}