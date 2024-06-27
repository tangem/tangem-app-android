package com.tangem.core.decompose.utils

import kotlinx.coroutines.CoroutineScope

/**
 * Interface for owning a component scope.
 */
interface ComponentScopeOwner {

    /**
     * Provides access to the component's [CoroutineScope] instance.
     *
     * This scope is used for launching coroutines that are bound to the component's lifecycle.
     */
    val componentScope: CoroutineScope
}