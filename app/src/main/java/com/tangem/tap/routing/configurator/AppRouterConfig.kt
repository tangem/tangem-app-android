package com.tangem.tap.routing.configurator

import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.navigation.Router
import kotlinx.coroutines.CoroutineScope

internal interface AppRouterConfig {

    var routerScope: CoroutineScope?
    var componentRouter: Router?
    var stack: List<AppRoute>?
}
