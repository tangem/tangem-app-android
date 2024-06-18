package com.tangem.features.details.component.preview

import com.tangem.core.decompose.navigation.Route
import com.tangem.core.decompose.navigation.Router

internal class PreviewRouter : Router {
    override fun push(route: Route, onComplete: (isSuccess: Boolean) -> Unit) {
        /* no-op */
    }

    override fun pop(onComplete: (isSuccess: Boolean) -> Unit) {
        /* no-op */
    }

    override fun popTo(route: Route, onComplete: (isSuccess: Boolean) -> Unit) {
        /* no-op */
    }
}