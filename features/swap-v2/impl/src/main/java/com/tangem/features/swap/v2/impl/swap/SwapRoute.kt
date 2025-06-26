package com.tangem.features.swap.v2.impl.swap

import com.tangem.core.decompose.navigation.Route
import kotlinx.serialization.Serializable

internal sealed class SwapRoute : Route {

    abstract val isEditMode: Boolean

    data object Empty : SwapRoute() {
        override val isEditMode: Boolean = false
    }

    @Serializable
    data object Confirm : SwapRoute() {
        override val isEditMode: Boolean = true
    }

    @Serializable
    data class Amount(
        override val isEditMode: Boolean,
    ) : SwapRoute()
}