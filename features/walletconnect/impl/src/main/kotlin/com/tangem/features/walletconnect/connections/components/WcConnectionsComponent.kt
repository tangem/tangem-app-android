package com.tangem.features.walletconnect.connections.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.tangem.core.ui.message.EventMessageEffect
import com.tangem.core.ui.message.EventMessageHandler
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.walletconnect.connections.model.WcConnectionsModel
import com.tangem.features.walletconnect.connections.routes.WcConnectionsBottomSheetConfig
import com.tangem.features.walletconnect.connections.ui.WcConnectionsContent

internal class WcConnectionsComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    private val messageHandler = EventMessageHandler()
    private val model: WcConnectionsModel = getOrCreateModel(params)
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
        val snackbarHostState = remember { SnackbarHostState() }

        WcConnectionsContent(modifier = modifier, state = state, snackbarHostState = snackbarHostState)

        EventMessageEffect(messageHandler = messageHandler, snackbarHostState = snackbarHostState)
        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(
        config: WcConnectionsBottomSheetConfig,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent {
        val context = childByContext(componentContext = componentContext, messageHandler = messageHandler)
        return when (config) {
            is WcConnectionsBottomSheetConfig.ConnectedApp -> WcConnectedAppInfoComponent(
                appComponentContext = context,
                params = WcConnectedAppInfoComponent.Params(
                    topic = config.topic,
                    onDismiss = model.bottomSheetNavigation::dismiss,
                ),
            )
        }
    }

    data class Params(val userWalletId: UserWalletId)
}