package com.tangem.tap.routing.configurator

import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.navigation.Router
import com.tangem.tap.common.SnackbarHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

internal class MutableAppRouterConfig : AppRouterConfig {
    override var routerScope: CoroutineScope? = null
    override var componentRouter: Router? = null
    override var stack: List<AppRoute>? = null
    override var snackbarHandler: SnackbarHandler? = null
    override val isInitialized: MutableStateFlow<Boolean> = MutableStateFlow(false)
}