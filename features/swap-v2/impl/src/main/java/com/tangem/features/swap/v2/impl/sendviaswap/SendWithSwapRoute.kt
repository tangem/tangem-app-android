package com.tangem.features.swap.v2.impl.sendviaswap

import com.tangem.core.decompose.navigation.Route
import com.tangem.features.send.v2.api.subcomponents.destination.DestinationRoute
import com.tangem.features.swap.v2.impl.amount.SwapAmountRoute
import kotlinx.serialization.Serializable

internal sealed class SendWithSwapRoute : Route {

    abstract val isEditMode: Boolean

    @Serializable
    data class Amount(
        override val isEditMode: Boolean,
    ) : SendWithSwapRoute(), SwapAmountRoute

    @Serializable
    data class Destination(
        override val isEditMode: Boolean,
    ) : SendWithSwapRoute(), DestinationRoute

    @Serializable
    data object Confirm : SendWithSwapRoute() {
        override val isEditMode: Boolean = false
    }

    @Serializable
    data object Success : SendWithSwapRoute() {
        override val isEditMode: Boolean = false
    }
}