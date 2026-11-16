package com.tangem.features.earn.impl.getfunds

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.features.earn.impl.getfunds.ui.EarnGetTokenSheetContent
import com.tangem.features.tokenreceive.TokenReceiveComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch

internal class EarnGetTokenSheetComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: Params,
    private val tokenReceiveFactory: TokenReceiveComponent.Factory,
) : ComposableBottomSheetComponent, AppComponentContext by context {

    private val model: EarnGetTokenSheetModel = getOrCreateModel(params)

    private val receiveNavigation = SlotNavigation<TokenReceiveConfig>()

    private val receiveSlot = childSlot(
        source = receiveNavigation,
        serializer = null,
        key = "getTokenReceive",
        handleBackButton = false,
        childFactory = { config, factoryContext ->
            tokenReceiveFactory.create(
                context = childByContext(componentContext = factoryContext),
                params = TokenReceiveComponent.Params(config = config, onDismiss = receiveNavigation::dismiss),
            )
        },
    )

    init {
        componentScope.launch { model.receiveConfigs.collect(receiveNavigation::activate) }
    }

    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.uiState.collectAsStateWithLifecycle()
        val receive by receiveSlot.subscribeAsState()

        state?.let { EarnGetTokenSheetContent(state = it) }
        receive.child?.instance?.BottomSheet()
    }

    data class Params(
        val request: EarnGetTokenSheetRequest,
        val onDismiss: () -> Unit,
    )

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext, params: Params): EarnGetTokenSheetComponent
    }
}