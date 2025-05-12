package com.tangem.features.send.v2.common

import com.tangem.core.decompose.navigation.Route
import kotlinx.serialization.Serializable

internal sealed class CommonSendRoute : Route {

    abstract val isEditMode: Boolean

    @Serializable
    data object Empty : CommonSendRoute() {
        override val isEditMode = false
    }

    @Serializable
    data object Confirm : CommonSendRoute() {
        override val isEditMode: Boolean = true
    }

    @Serializable
    data class Destination(
        override val isEditMode: Boolean,
    ) : CommonSendRoute()

    @Serializable
    data class Amount(
        override val isEditMode: Boolean,
    ) : CommonSendRoute()

    @Serializable
    data object Fee : CommonSendRoute() {
        override val isEditMode: Boolean = true
    }
}