package com.tangem.tap.routing.configurator

import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.navigation.Router
import kotlinx.coroutines.CoroutineScope

internal class MutableAppRouterConfig : AppRouterConfig {

    override var routerScope: CoroutineScope? = null
    override var componentRouter: Router? = null
    override var stack: List<AppRoute>? = null
}