package com.tangem.features.walletconnect.connections.components

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.walletconnect.connections.model.WcConnectedAppInfoModel
import com.tangem.features.walletconnect.connections.routes.ConnectedAppInfoRoutes
import com.tangem.features.walletconnect.connections.utils.WcAlertsFactory

internal class WcConnectedAppInfoContainerComponent(
    params: Params,
    appComponentContext: AppComponentContext,
) : AppComponentContext by appComponentContext, ComposableBottomSheetComponent {

    private val model: WcConnectedAppInfoModel = getOrCreateModel(params = params)
    private val innerRouter = InnerRouter<ConnectedAppInfoRoutes>(
        stackNavigation = model.stackNavigation,
        popCallback = { onChildBack() },
    )
    private val contentStack = childStack(
        key = "WcConnectedAppInfoStack",
        source = model.stackNavigation,
        serializer = ConnectedAppInfoRoutes.serializer(),
        initialConfiguration = ConnectedAppInfoRoutes.AppInfo,
        handleBackButton = true,
        childFactory = ::screenChild,
    )

    @Composable
    override fun BottomSheet() {
        val content by contentStack.subscribeAsState()

        BackHandler(onBack = ::onChildBack)
        content.active.instance.BottomSheet()
    }

    override fun dismiss() {
        router.pop()
    }

    private fun onChildBack() {
        when (contentStack.value.active.configuration) {
            is ConnectedAppInfoRoutes.AppInfo -> dismiss()
            is ConnectedAppInfoRoutes.VerifiedAlert -> model.stackNavigation.pop()
        }
    }

    private fun screenChild(
        config: ConnectedAppInfoRoutes,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent {
        val appComponentContext = childByContext(
            componentContext = componentContext,
            router = innerRouter,
        )
        return when (config) {
            is ConnectedAppInfoRoutes.AppInfo -> WcConnectedAppInfoComponent(
                appComponentContext = appComponentContext,
                model = model,
            )
            is ConnectedAppInfoRoutes.VerifiedAlert -> AlertsComponentV2(
                appComponentContext = appComponentContext,
                messageUM = WcAlertsFactory.createVerifiedDomainAlert(config.appName),
            )
        }
    }

    data class Params(val topic: String, val onDismiss: () -> Unit)
}