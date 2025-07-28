package com.tangem.features.pushnotifications.api

import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface PushNotificationsComponent : ComposableContentComponent {

    interface Factory : ComponentFactory<Params, PushNotificationsComponent>

    interface ModelCallbacks {
        fun onResult()
    }

    sealed class Params {
        data class Callbacks(val callbacks: ModelCallbacks) : Params()
        data class Route(val route: AppRoute) : Params()
    }
}