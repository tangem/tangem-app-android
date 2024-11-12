package com.tangem.core.decompose.navigation.inner

import kotlinx.coroutines.flow.StateFlow

/**
 * Interface to provide internal navigation access from child to parent
 */
interface InnerNavigation {
    val state: StateFlow<InnerNavigationState>
    fun pop(onComplete: (Boolean) -> Unit)
}

interface InnerNavigationState {
    val stackSize: Int
    val stackMaxSize: Int?
}