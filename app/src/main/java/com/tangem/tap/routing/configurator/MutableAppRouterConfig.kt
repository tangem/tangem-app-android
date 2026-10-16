package com.tangem.tap.routing.configurator

import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.navigation.Router
import com.tangem.tap.common.SnackbarHandler
import kotlinx.coroutines.flow.MutableStateFlow

internal class MutableAppRouterConfig : AppRouterConfig {
    // Written on the main thread by the root component, read by app-level singletons from any thread
    @Volatile
    override var componentRouter: Router? = null

    @Volatile
    override var stack: List<AppRoute>? = null
    override var snackbarHandler: SnackbarHandler? = null
    override val initializedState: MutableStateFlow<Boolean> = MutableStateFlow(false)
}