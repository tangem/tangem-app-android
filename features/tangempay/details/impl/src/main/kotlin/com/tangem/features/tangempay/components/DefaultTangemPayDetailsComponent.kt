package com.tangem.features.tangempay.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.NavigationBar3ButtonsScrim
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.tangempay.components.txHistory.DefaultTangemPayTxHistoryComponent
import com.tangem.features.tangempay.model.TangemPayDetailsModel
import com.tangem.features.tangempay.model.TangemPayDetailsNavigation
import com.tangem.features.tangempay.ui.TangemPayDetailsScreen
import com.tangem.features.tokenreceive.TokenReceiveComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultTangemPayDetailsComponent @AssistedInject constructor(
    @Assisted private val appComponentContext: AppComponentContext,
    @Assisted private val params: TangemPayDetailsComponent.Params,
    private val tokenReceiveComponentFactory: TokenReceiveComponent.Factory,
) : AppComponentContext by appComponentContext, TangemPayDetailsComponent {

    private val model: TangemPayDetailsModel = getOrCreateModel(params = params)

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = TangemPayDetailsNavigation.serializer(),
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )
    private val txHistoryComponent = DefaultTangemPayTxHistoryComponent(
        appComponentContext = child("txHistoryComponent"),
        params = DefaultTangemPayTxHistoryComponent.Params(customerWalletAddress = params.config.customerWalletAddress),
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        NavigationBar3ButtonsScrim()
        TangemPayDetailsScreen(
            state = state,
            txHistoryComponent = txHistoryComponent,
            modifier = modifier,
        )
        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(
        navigation: TangemPayDetailsNavigation,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent = when (navigation) {
        is TangemPayDetailsNavigation.Error -> TangemPayErrorBottomSheetComponent(
            appComponentContext = appComponentContext,
            messageUM = navigation.messageUM,
            onDismiss = model.bottomSheetNavigation::dismiss,
        )
        is TangemPayDetailsNavigation.Receive -> tokenReceiveComponentFactory.create(
            context = childByContext(componentContext),
            params = TokenReceiveComponent.Params(
                config = navigation.config,
                onDismiss = model.bottomSheetNavigation::dismiss,
            ),
        )
    }

    @AssistedFactory
    interface Factory : TangemPayDetailsComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: TangemPayDetailsComponent.Params,
        ): DefaultTangemPayDetailsComponent
    }
}