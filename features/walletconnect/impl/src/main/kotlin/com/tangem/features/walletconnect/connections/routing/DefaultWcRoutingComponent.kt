package com.tangem.features.walletconnect.connections.routing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.subscribe
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.walletconnect.components.WcRoutingComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Suppress("UnusedPrivateMember")
internal class DefaultWcRoutingComponent @AssistedInject constructor(
    @Assisted private val appComponentContext: AppComponentContext,
    @Assisted params: Unit,
) : AppComponentContext by appComponentContext, WcRoutingComponent {

    private val model: WcRoutingModel = getOrCreateModel()

    private val slot: Value<ChildSlot<WcInnerRoute, ComposableContentComponent>> = childSlot(
        source = model.innerRouter.slotNavigation,
        serializer = WcInnerRoute.serializer(),
        childFactory = ::childFactory,
    )

    init {
        slot.subscribe(lifecycle) { slot -> if (slot.child == null) model.onSlotEmpty() }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val slot by slot.subscribeAsState()
        slot.child?.instance?.Content(modifier)
    }

    override fun onAppRouteChange(appRoute: AppRoute) {
        model.onAppRouteChange(appRoute)
    }

    private fun childFactory(config: WcInnerRoute, componentContext: ComponentContext): ComposableContentComponent {
        val childContext = childByContext(
            componentContext = componentContext,
            router = model.innerRouter,
        )
        return when (config) {
            is WcInnerRoute.SignMessage -> TODO()
            is WcInnerRoute.Pair -> TODO()
        }
    }

    @AssistedFactory
    interface Factory : WcRoutingComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultWcRoutingComponent
    }
}