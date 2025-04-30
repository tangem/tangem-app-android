package com.tangem.features.walletconnect.components

import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface WcRoutingComponent : ComposableContentComponent {

    fun onAppRouteChange(appRoute: AppRoute)

    interface Factory : ComponentFactory<Unit, WcRoutingComponent>
}