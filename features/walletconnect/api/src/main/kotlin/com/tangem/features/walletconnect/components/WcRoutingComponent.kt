package com.tangem.features.walletconnect.components

import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.value.Value
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.ui.decompose.ComposableContentComponent

interface WcRoutingComponent : ComposableContentComponent {

    val slot: Value<ChildSlot<Route, ComposableContentComponent>>

    fun onAppRouteChange(appRoute: AppRoute)

    interface Factory : ComponentFactory<Unit, WcRoutingComponent>
}