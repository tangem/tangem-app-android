package com.tangem.features.walletconnect.connections.components

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUMV2
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.walletconnect.model.WcPairRequest
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.walletconnect.connections.model.WcPairModel
import com.tangem.features.walletconnect.connections.routes.WcAppInfoRoutes
import com.tangem.features.walletconnect.connections.routes.WcAppInfoRoutes.Alert
import com.tangem.features.walletconnect.connections.utils.WcAlertsFactory

internal class WcPairComponent(
    private val appComponentContext: AppComponentContext,
    params: Params,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    private val model: WcPairModel = getOrCreateModel(params = params)
    private val innerRouter = InnerRouter<WcAppInfoRoutes>(
        stackNavigation = model.stackNavigation,
        popCallback = { onChildBack() },
    )
    private val contentStack = childStack(
        key = "WcConnectionFlowStack",
        source = model.stackNavigation,
        serializer = WcAppInfoRoutes.serializer(),
        initialConfiguration = WcAppInfoRoutes.AppInfo,
        handleBackButton = true,
        childFactory = ::screenChild,
    )

    private fun dismiss() {
        model.rejectPairing()
        router.pop()
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val content by contentStack.subscribeAsState()

        BackHandler(onBack = ::onChildBack)
        content.active.instance.BottomSheet()
    }

    private fun onChildBack() {
        when (val config = contentStack.value.active.configuration) {
            is WcAppInfoRoutes.AppInfo -> dismiss()
            is Alert -> when (config.alertType) {
                is Alert.Type.UnsupportedDApp,
                is Alert.Type.UnsupportedNetwork,
                -> dismiss()
                else -> model.stackNavigation.pop()
            }
            is WcAppInfoRoutes.SelectNetworks,
            is WcAppInfoRoutes.SelectWallet,
            -> model.stackNavigation.pop()
        }
    }

    private fun screenChild(
        config: WcAppInfoRoutes,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent {
        val appComponentContext = childByContext(
            componentContext = componentContext,
            router = innerRouter,
        )
        return when (config) {
            is WcAppInfoRoutes.AppInfo -> WcAppInfoComponent(
                appComponentContext = appComponentContext,
                onDismiss = ::dismiss,
                model = model,
            )
            is Alert -> AlertsComponentV2(
                appComponentContext = appComponentContext,
                messageUM = createBottomSheetMessageUM(config.alertType),
            )
            is WcAppInfoRoutes.SelectNetworks -> WcSelectNetworksComponent(
                appComponentContext = appComponentContext,
                params = WcSelectNetworksComponent.WcSelectNetworksParams(
                    missingRequiredNetworks = config.missingRequiredNetworks,
                    requiredNetworks = config.requiredNetworks,
                    availableNetworks = config.availableNetworks,
                    notAddedNetworks = config.notAddedNetworks,
                    enabledAvailableNetworks = config.enabledAvailableNetworks,
                    onDismiss = ::dismiss,
                    callback = model,
                ),
            )
            is WcAppInfoRoutes.SelectWallet -> WcSelectWalletComponent(
                appComponentContext = appComponentContext,
                params = WcSelectWalletComponent.WcSelectWalletParams(
                    selectedWalletId = config.selectedWalletId,
                    onDismiss = ::dismiss,
                    callback = model,
                ),
            )
        }
    }

    private fun createBottomSheetMessageUM(alertType: Alert.Type): MessageBottomSheetUMV2 {
        return when (alertType) {
            is Alert.Type.Verified -> WcAlertsFactory.createVerifiedDomainAlert(alertType.appName)
            is Alert.Type.UnknownDomain -> WcAlertsFactory.createUnknownDomainAlert(model::connectFromAlert)
            is Alert.Type.UnsafeDomain -> WcAlertsFactory.createUnsafeDomainAlert(model::connectFromAlert)
            is Alert.Type.UnsupportedDApp ->
                WcAlertsFactory.createUnsupportedDomainAlert(alertType.appName, model::errorAlertOnDismiss)
            is Alert.Type.UnsupportedNetwork ->
                WcAlertsFactory.createUnsupportedChainAlert(alertType.appName, model::errorAlertOnDismiss)
            is Alert.Type.UriAlreadyUsed -> WcAlertsFactory.createUriAlreadyUsedAlert(model::errorAlertOnDismiss)
        }
    }

    data class Params(val userWalletId: UserWalletId, val wcUrl: String, val source: WcPairRequest.Source)
}