package com.tangem.features.send.v2.send

import com.tangem.core.decompose.navigation.Route
import kotlinx.serialization.Serializable

@Serializable
internal sealed class SendRoute : Route {

    abstract val isEditMode: Boolean

    @Serializable
    data object Empty : SendRoute() {
        override val isEditMode = false
    }

    @Serializable
    data object Confirm : SendRoute() {
        override val isEditMode: Boolean = false
    }

    @Serializable
    data class Destination(
        override val isEditMode: Boolean,
    ) : SendRoute()

    @Serializable
    data class Amount(
        override val isEditMode: Boolean,
    ) : SendRoute()

    @Serializable
    data class Fee(
        override val isEditMode: Boolean = true,
    ) : SendRoute()
}