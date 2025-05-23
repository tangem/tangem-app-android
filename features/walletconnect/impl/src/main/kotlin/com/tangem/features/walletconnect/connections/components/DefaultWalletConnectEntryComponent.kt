package com.tangem.features.walletconnect.connections.components

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.walletconnect.components.WalletConnectEntryComponent
import com.tangem.features.walletconnect.connections.routes.ConnectionsInnerRoute
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Suppress("UnusedPrivateMember")
internal class DefaultWalletConnectEntryComponent @AssistedInject constructor(
    @Assisted private val appComponentContext: AppComponentContext,
    @Assisted private val params: WalletConnectEntryComponent.Params,
) : AppComponentContext by appComponentContext, WalletConnectEntryComponent {

    private val contentNavigation = StackNavigation<ConnectionsInnerRoute>()
    private val contentStack = childStack(
        source = contentNavigation,
        serializer = ConnectionsInnerRoute.serializer(),
        initialConfiguration = ConnectionsInnerRoute.Connections,
        childFactory = ::screenChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val childStack by contentStack.subscribeAsState()

        BackHandler(onBack = router::pop)
        Children(stack = childStack, animation = stackAnimation()) { child ->
            child.instance.Content(modifier = modifier)
        }
    }

    private fun screenChild(
        config: ConnectionsInnerRoute,
        componentContext: ComponentContext,
    ): ComposableContentComponent = when (config) {
        ConnectionsInnerRoute.Connections -> ConnectionsComponent(
            appComponentContext = childByContext(componentContext),
            params = ConnectionsComponent.Params(params.userWalletId),
        )
        ConnectionsInnerRoute.QrScan -> TODO("[REDACTED_JIRA]")
    }

    @AssistedFactory
    interface Factory : WalletConnectEntryComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: WalletConnectEntryComponent.Params,
        ): DefaultWalletConnectEntryComponent
    }
}