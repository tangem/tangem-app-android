package com.tangem.features.walletconnect.connections.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.walletconnect.model.WcPairRequest
import com.tangem.features.walletconnect.connections.model.WcConnectionsModel
import com.tangem.features.walletconnect.connections.routes.WcConnectionsBottomSheetRoutes
import com.tangem.features.walletconnect.connections.ui.WcConnectionsContent

internal class ConnectionsComponent(
    appComponentContext: AppComponentContext,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    private val model: WcConnectionsModel = getOrCreateModel()
    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = null,
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()
        WcConnectionsContent(modifier = modifier, state = state)

        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(
        config: WcConnectionsBottomSheetRoutes,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent {
        return when (config) {
            is WcConnectionsBottomSheetRoutes.AppInfo -> WcAppInfoContainerComponent(
                appComponentContext = childByContext(componentContext),
                params = WcAppInfoContainerComponent.Params(
                    wcUrl = config.wcUrl,
                    source = WcPairRequest.Source.QR,
                    onDismiss = model.bottomSheetNavigation::dismiss,
                ),
            )
        }
    }
}