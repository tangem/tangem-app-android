package com.tangem.core.decompose.navigation

import kotlin.reflect.KClass

class DummyRouter : Router {

    override fun push(route: Route, onComplete: (isSuccess: Boolean) -> Unit) {
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
}
