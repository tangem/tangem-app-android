package com.tangem.core.decompose.navigation

import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.essenty.instancekeeper.InstanceKeeper

internal class DefaultAppNavigationProvider : AppNavigationProvider, InstanceKeeper.Instance {

    private var navigation: StackNavigation<Route>? = null

    @Volatile
    override var stack: List<Route> = emptyList()
        private set

    override fun getOrCreate(): StackNavigation<Route> {
        return navigation ?: StackNavigation<Route>().also { navigation = it }
    }

    override fun updateStack(stack: List<Route>) {
        this.stack = stack
    }
}