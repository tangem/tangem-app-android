package com.tangem.common.routing.utils

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter

/**
 * Pops routes from the navigation stack until the specified route [R] is found.
 *
 * ***Must be removed after Decompose migration.***
 *
 * @param R The route to pop to.
 * @param onComplete The callback to be invoked when the operation is complete.
 */
inline fun <reified R : AppRoute> AppRouter.popTo(noinline onComplete: (isSuccess: Boolean) -> Unit = {}) {
    popTo(R::class, onComplete)
}
