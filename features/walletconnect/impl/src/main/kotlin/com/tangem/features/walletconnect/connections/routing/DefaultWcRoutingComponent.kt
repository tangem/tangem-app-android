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
import com.tangem.features.walletconnect.connections.components.WcPairComponent
import com.tangem.features.walletconnect.connections.components.AlertsComponent
import com.tangem.features.walletconnect.transaction.components.chain.WcAddNetworkContainerComponent
import com.tangem.features.walletconnect.transaction.components.common.WcTransactionModelParams
import com.tangem.features.walletconnect.transaction.components.send.WcSendTransactionContainerComponent
import com.tangem.features.walletconnect.transaction.components.sign.WcSignTransactionContainerComponent
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
            is WcInnerRoute.SignMessage -> WcSignTransactionContainerComponent(
                appComponentContext = childContext,
                params = WcTransactionModelParams(config.rawRequest),
            )
            is WcInnerRoute.AddNetwork -> WcAddNetworkContainerComponent(
                appComponentContext = childContext,
                params = WcTransactionModelParams(config.rawRequest),
            )
            is WcInnerRoute.Send -> WcSendTransactionContainerComponent(
                appComponentContext = childContext,
                params = WcTransactionModelParams(config.rawRequest),
            )
            is WcInnerRoute.Pair -> WcPairComponent(
                appComponentContext = childContext,
                params = WcPairComponent.Params(
                    userWalletId = config.request.userWalletId,
                    wcUrl = config.request.uri,
                    source = config.request.source,
                ),
            )
            is WcInnerRoute.Alert -> AlertsComponent(
                childContext,
                AlertsComponent.Params(
                    alertType = config.alertType,
                ),
            )
        }
    }

    @AssistedFactory
    interface Factory : WcRoutingComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultWcRoutingComponent
    }
}